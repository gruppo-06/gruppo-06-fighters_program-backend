package it.fightersprogram;

import static org.hamcrest.Matchers.is;
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

@SpringBootTest(properties = "game.starter-collection.enabled=false")
@AutoConfigureMockMvc
@Transactional
class TradeApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
