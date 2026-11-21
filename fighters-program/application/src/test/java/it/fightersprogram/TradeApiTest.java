package it.fightersprogram;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = "game.starter-collection.enabled=false")
@AutoConfigureMockMvc
@Transactional
class TradeApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void proposesTradeWithCardDetails() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "trade_alice");
        TestUsers.registerAndLogin(mockMvc, "trade_bob");
        long aliceId = userId("trade_alice");
        long bobId = userId("trade_bob");
        long offered = cardId("EA01");
        long requested = cardId("EA02");
        own(aliceId, offered, 2);
        own(bobId, requested, 1);

        mockMvc.perform(post("/api/trades").header(HttpHeaders.AUTHORIZATION, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("trade_bob", offered, 2, requested, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.senderNickname", is("trade_alice")))
                .andExpect(jsonPath("$.recipientNickname", is("trade_bob")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.offered[0].cardId", is((int) offered)))
                .andExpect(jsonPath("$.offered[0].code", is("EA01")))
                .andExpect(jsonPath("$.offered[0].name", is("Fighter EA01")))
                .andExpect(jsonPath("$.offered[0].quantity", is(2)))
                .andExpect(jsonPath("$.requested[0].code", is("EA02")))
                .andExpect(jsonPath("$.requested[0].quantity", is(1)))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.resolvedAt").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void rejectsInvalidLinesAndEmptySides() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "trade_validation");
        String[] requests = {
                "{\"recipientNickname\":\"nobody\",\"offered\":[],\"requested\":[{\"cardId\":1,\"quantity\":1}]}",
                "{\"recipientNickname\":\"nobody\",\"offered\":[{\"cardId\":1,\"quantity\":0}],\"requested\":[{\"cardId\":2,\"quantity\":1}]}",
                "{\"recipientNickname\":\"nobody\",\"offered\":[{\"cardId\":1,\"quantity\":1},{\"cardId\":1,\"quantity\":1}],\"requested\":[{\"cardId\":2,\"quantity\":1}]}"
        };
        for (String request : requests) {
            mockMvc.perform(post("/api/trades").header(HttpHeaders.AUTHORIZATION, alice)
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        }
    }

    @Test
    void rejectsSelfAndUnknownRecipient() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "trade_self");
        String body = request("trade_self", 1, 1, 2, 1);
        mockMvc.perform(post("/api/trades").header(HttpHeaders.AUTHORIZATION, alice)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code", is("BAD_REQUEST")));
        mockMvc.perform(post("/api/trades").header(HttpHeaders.AUTHORIZATION, alice)
                        .contentType(MediaType.APPLICATION_JSON).content(request("absent", 1, 1, 2, 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAvailableOwnedCardsOnBothSides() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "trade_owner");
        TestUsers.registerAndLogin(mockMvc, "trade_receiver");
        long aliceId = userId("trade_owner");
        long bobId = userId("trade_receiver");
        long offered = cardId("EA01");
        long requested = cardId("EA02");
        own(aliceId, offered, 2);
        own(bobId, requested, 1);
        String url = "/api/trades";

        mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, alice).contentType(MediaType.APPLICATION_JSON)
                        .content(request("trade_receiver", offered, 1, requested, 1)))
                .andExpect(status().isCreated());
        mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, alice).contentType(MediaType.APPLICATION_JSON)
                        .content(request("trade_receiver", offered, 2, requested, 1)))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, alice).contentType(MediaType.APPLICATION_JSON)
                        .content(request("trade_receiver", offered, 3, requested, 1)))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, alice).contentType(MediaType.APPLICATION_JSON)
                        .content(request("trade_receiver", offered, 1, requested, 2)))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, alice).contentType(MediaType.APPLICATION_JSON)
                        .content(request("trade_receiver", 999999, 1, requested, 1)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void listsOnlyRelevantTradesAndHidesDetailsFromThirdParties() throws Exception {
        String sender = TestUsers.registerAndLogin(mockMvc, "trade_list_sender");
        String recipient = TestUsers.registerAndLogin(mockMvc, "trade_list_recipient");
        String outsider = TestUsers.registerAndLogin(mockMvc, "trade_list_outsider");
        long senderId = userId("trade_list_sender");
        long recipientId = userId("trade_list_recipient");
        long offered = cardId("EA01");
        long requested = cardId("EA02");
        own(senderId, offered, 5);
        own(recipientId, requested, 5);

        long olderId = createTrade(sender, "trade_list_recipient", offered, requested);
        long newerId = createTrade(sender, "trade_list_recipient", offered, requested);
        jdbcTemplate.update("UPDATE trade SET created_at = now() - interval '2 minutes' WHERE id = ?", olderId);
        jdbcTemplate.update("UPDATE trade SET created_at = now() - interval '1 minute' WHERE id = ?", newerId);
        jdbcTemplate.update("UPDATE trade SET status = 'ACCEPTED', resolved_at = now() WHERE id = ?", newerId);

        mockMvc.perform(get("/api/trades/incoming").header(HttpHeaders.AUTHORIZATION, recipient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is((int) olderId)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
        mockMvc.perform(get("/api/trades/outgoing").header(HttpHeaders.AUTHORIZATION, sender))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is((int) newerId)))
                .andExpect(jsonPath("$[0].status", is("ACCEPTED")))
                .andExpect(jsonPath("$[1].id", is((int) olderId)));
        mockMvc.perform(get("/api/trades/incoming").header(HttpHeaders.AUTHORIZATION, sender))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/trades/outgoing").header(HttpHeaders.AUTHORIZATION, recipient))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/trades/incoming").header(HttpHeaders.AUTHORIZATION, outsider))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/trades/outgoing").header(HttpHeaders.AUTHORIZATION, outsider))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        String detail = "/api/trades/" + olderId;
        mockMvc.perform(get(detail).header(HttpHeaders.AUTHORIZATION, sender))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id", is((int) olderId)));
        mockMvc.perform(get(detail).header(HttpHeaders.AUTHORIZATION, recipient))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id", is((int) olderId)));
        mockMvc.perform(get(detail).header(HttpHeaders.AUTHORIZATION, outsider))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptingTransfersCardsAtomicallyAndOnlyTheRecipientCanAccept() throws Exception {
        String sender = TestUsers.registerAndLogin(mockMvc, "trade_accept_sender");
        String recipient = TestUsers.registerAndLogin(mockMvc, "trade_accept_recipient");
        String outsider = TestUsers.registerAndLogin(mockMvc, "trade_accept_outsider");
        long senderId = userId("trade_accept_sender");
        long recipientId = userId("trade_accept_recipient");
        long offered = cardId("EA01");
        long requested = cardId("EA02");
        own(senderId, offered, 1);
        own(recipientId, requested, 1);
        long tradeId = createTrade(sender, "trade_accept_recipient", offered, requested);
        int offeredTotal = totalCopies(offered);
        int requestedTotal = totalCopies(requested);

        mockMvc.perform(post("/api/trades/" + tradeId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, sender))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/trades/" + tradeId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, outsider))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/trades/" + tradeId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, recipient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        entityManager.flush();
        org.junit.jupiter.api.Assertions.assertEquals(1, quantity(senderId, requested));
        org.junit.jupiter.api.Assertions.assertEquals(1, quantity(recipientId, offered));
        org.junit.jupiter.api.Assertions.assertEquals(0, quantity(senderId, offered));
        org.junit.jupiter.api.Assertions.assertEquals(0, quantity(recipientId, requested));
        org.junit.jupiter.api.Assertions.assertEquals(offeredTotal, totalCopies(offered));
        org.junit.jupiter.api.Assertions.assertEquals(requestedTotal, totalCopies(requested));
        mockMvc.perform(get("/api/me/collection").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, sender))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", not(hasItem("EA01"))))
                .andExpect(jsonPath("$.content[*].code", hasItem("EA02")));

        mockMvc.perform(post("/api/trades/" + tradeId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, recipient))
                .andExpect(status().isConflict());
    }

    @Test
    void acceptingFailsWithoutChangingCardsOrTradeWhenSenderNoLongerHasCopies() throws Exception {
        String sender = TestUsers.registerAndLogin(mockMvc, "trade_stale_sender");
        String recipient = TestUsers.registerAndLogin(mockMvc, "trade_stale_recipient");
        long senderId = userId("trade_stale_sender");
        long recipientId = userId("trade_stale_recipient");
        long offered = cardId("EA01");
        long requested = cardId("EA02");
        own(senderId, offered, 2);
        own(recipientId, requested, 1);
        long tradeId = createTrade(sender, "trade_stale_recipient", offered, requested, 2);
        entityManager.flush();
        jdbcTemplate.update("UPDATE user_card SET quantity = 1 WHERE user_id = ? AND card_id = ?",
                senderId, offered);
        entityManager.clear();

        mockMvc.perform(post("/api/trades/" + tradeId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, recipient))
                .andExpect(status().isUnprocessableContent());

        entityManager.flush();
        org.junit.jupiter.api.Assertions.assertEquals("PENDING", jdbcTemplate.queryForObject(
                "SELECT status FROM trade WHERE id = ?", String.class, tradeId));
        org.junit.jupiter.api.Assertions.assertEquals(1, quantity(senderId, offered));
        org.junit.jupiter.api.Assertions.assertEquals(1, quantity(recipientId, requested));
        org.junit.jupiter.api.Assertions.assertEquals(0, quantity(senderId, requested));
        org.junit.jupiter.api.Assertions.assertEquals(0, quantity(recipientId, offered));
    }

    @Test
    void onlyRecipientCanDeclinePendingTradeAndDeclineDoesNotMoveCards() throws Exception {
        String sender = TestUsers.registerAndLogin(mockMvc, "trade_decline_sender");
        String recipient = TestUsers.registerAndLogin(mockMvc, "trade_decline_recipient");
        String outsider = TestUsers.registerAndLogin(mockMvc, "trade_decline_outsider");
        long senderId = userId("trade_decline_sender");
        long recipientId = userId("trade_decline_recipient");
        long offered = cardId("EA01");
        long requested = cardId("EA02");
        own(senderId, offered, 1);
        own(recipientId, requested, 1);
        long tradeId = createTrade(sender, "trade_decline_recipient", offered, requested);

        mockMvc.perform(post("/api/trades/" + tradeId + "/decline")
                        .header(HttpHeaders.AUTHORIZATION, sender))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/trades/" + tradeId + "/decline")
                        .header(HttpHeaders.AUTHORIZATION, outsider))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/trades/" + tradeId + "/decline")
                        .header(HttpHeaders.AUTHORIZATION, recipient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DECLINED")))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        org.junit.jupiter.api.Assertions.assertEquals(1, quantity(senderId, offered));
        org.junit.jupiter.api.Assertions.assertEquals(1, quantity(recipientId, requested));
        mockMvc.perform(post("/api/trades/" + tradeId + "/decline")
                        .header(HttpHeaders.AUTHORIZATION, recipient))
                .andExpect(status().isConflict());
    }

    @Test
    void onlySenderCanCancelAndCancelledOfferCanBeProposedAgain() throws Exception {
        String sender = TestUsers.registerAndLogin(mockMvc, "trade_cancel_sender");
        String recipient = TestUsers.registerAndLogin(mockMvc, "trade_cancel_recipient");
        String outsider = TestUsers.registerAndLogin(mockMvc, "trade_cancel_outsider");
        long senderId = userId("trade_cancel_sender");
        long recipientId = userId("trade_cancel_recipient");
        long offered = cardId("EA01");
        long requested = cardId("EA02");
        own(senderId, offered, 1);
        own(recipientId, requested, 1);
        long tradeId = createTrade(sender, "trade_cancel_recipient", offered, requested);

        mockMvc.perform(post("/api/trades/" + tradeId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, recipient))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/trades/" + tradeId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, outsider))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/trades/" + tradeId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, sender))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        org.junit.jupiter.api.Assertions.assertEquals(1, quantity(senderId, offered));
        org.junit.jupiter.api.Assertions.assertEquals(1, quantity(recipientId, requested));
        mockMvc.perform(post("/api/trades").header(HttpHeaders.AUTHORIZATION, sender)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("trade_cancel_recipient", offered, 1, requested, 1)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/trades/" + tradeId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, sender))
                .andExpect(status().isConflict());
    }

    private long createTrade(String token, String recipient, long offered, long requested) throws Exception {
        return createTrade(token, recipient, offered, requested, 1);
    }

    private long createTrade(String token, String recipient, long offered, long requested, int offeredQuantity)
            throws Exception {
        String body = mockMvc.perform(post("/api/trades").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(recipient, offered, offeredQuantity, requested, 1)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.<Integer>read(body, "$.id").longValue();
    }

    private int quantity(long userId, long cardId) {
        Integer quantity = jdbcTemplate.query("SELECT quantity FROM user_card WHERE user_id = ? AND card_id = ?",
                result -> result.next() ? result.getInt(1) : null, userId, cardId);
        return quantity == null ? 0 : quantity;
    }

    private int totalCopies(long cardId) {
        return jdbcTemplate.queryForObject("SELECT COALESCE(sum(quantity), 0) FROM user_card WHERE card_id = ?",
                Integer.class, cardId);
    }

    private long userId(String nickname) {
        return jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE nickname = ?", Long.class, nickname);
    }

    private long cardId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM card WHERE code = ?", Long.class, code);
    }

    private void own(long userId, long cardId, int quantity) {
        jdbcTemplate.update("INSERT INTO user_card (user_id, card_id, quantity) VALUES (?, ?, ?)",
                userId, cardId, quantity);
    }

    private static String request(String recipient, long offered, int offeredQuantity,
            long requested, int requestedQuantity) {
        return """ 
                {"recipientNickname":"%s","offered":[{"cardId":%d,"quantity":%d}],"requested":[{"cardId":%d,"quantity":%d}]}
                """.formatted(recipient, offered, offeredQuantity, requested, requestedQuantity);
    }
}
