package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "game.starter-collection.enabled=false")
@AutoConfigureMockMvc
class TradeConcurrencyApiTest {

    private static final String TRADES = "/api/trades";
    private static final int CONCURRENCY_TIMEOUT_SECONDS = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Map<Long, Long> totalsBefore;

    @BeforeEach
    void captureCardTotals() {
        totalsBefore = cardTotals();
    }

    @AfterEach
    void cardTotalsAreUnchanged() {
        assertEquals(totalsBefore, cardTotals(), "Trade operations must conserve every card copy");
    }

    @Test
    void exchangesDifferentQuantitiesBetweenBothCollections() throws Exception {
        TestUser sender = register("sender");
        TestUser recipient = register("recipient");
        long senderId = userId(sender.nickname());
        long recipientId = userId(recipient.nickname());
        long threeCopiesCard = cardId("EA01");
        long oneCopyCard = cardId("EA02");
        own(senderId, threeCopiesCard, 3);
        own(recipientId, oneCopyCard, 1);

        long tradeId = propose(sender.token(), recipient.nickname(), threeCopiesCard, 3, oneCopyCard);
        mockMvc.perform(post(TRADES + "/" + tradeId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, recipient.token()))
                .andExpect(status().isOk());

        assertEquals(0, quantity(senderId, threeCopiesCard));
        assertEquals(3, quantity(recipientId, threeCopiesCard));
        assertEquals(1, quantity(senderId, oneCopyCard));
        assertEquals(0, quantity(recipientId, oneCopyCard));
    }

    @RepeatedTest(20)
    void onlyOneTradeCanConsumeTheRecipientsLastRequestedCopy() throws Exception {
        TestUser senderOne = register("a");
        TestUser recipient = register("b");
        TestUser senderTwo = register("c");
        long senderOneId = userId(senderOne.nickname());
        long recipientId = userId(recipient.nickname());
        long senderTwoId = userId(senderTwo.nickname());
        long requested = cardId("EA02");
        long offeredOne = cardId("EA01");
        long offeredTwo = cardId("EA03");
        own(senderOneId, offeredOne, 1);
        own(senderTwoId, offeredTwo, 1);
        own(recipientId, requested, 1);
        long firstTrade = propose(senderOne.token(), recipient.nickname(), offeredOne, 1, requested);
        long secondTrade = propose(senderTwo.token(), recipient.nickname(), offeredTwo, 1, requested);

        List<Integer> results = acceptTogether(
                new Acceptance(recipient.token(), firstTrade), new Acceptance(recipient.token(), secondTrade));

        assertEquals(1, results.stream().filter(status -> status == 200).count());
        assertEquals(1, results.stream().filter(status -> status == 422).count());
        assertEquals(0, quantity(recipientId, requested));
    }

    @RepeatedTest(20)
    void oppositeTradesFinishWithoutDeadlock() throws Exception {
        TestUser playerA = register("a");
        TestUser playerB = register("b");
        long playerAId = userId(playerA.nickname());
        long playerBId = userId(playerB.nickname());
        long cardA = cardId("EA01");
        long cardB = cardId("EA02");
        own(playerAId, cardA, 1);
        own(playerBId, cardB, 1);
        long tradeAtoB = propose(playerA.token(), playerB.nickname(), cardA, 1, cardB);
        long tradeBtoA = propose(playerB.token(), playerA.nickname(), cardB, 1, cardA);

        List<Integer> results = acceptTogether(
                new Acceptance(playerB.token(), tradeAtoB), new Acceptance(playerA.token(), tradeBtoA));

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(status -> status == 200 || status == 422),
                "Both acceptances should finish successfully or fail for insufficient cards");
    }

    private TestUser register(String role) throws Exception {
        String nickname = "tc" + role + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String token = TestUsers.registerAndLogin(mockMvc, nickname);
        return new TestUser(nickname, token);
    }

    private long propose(String token, String recipient, long offered, int offeredQuantity, long requested)
            throws Exception {
        String body = """
                {"recipientNickname":"%s","offered":[{"cardId":%d,"quantity":%d}],"requested":[{"cardId":%d,"quantity":1}]}
                """.formatted(recipient, offered, offeredQuantity, requested);
        MvcResult result = mockMvc.perform(post(TRADES).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private List<Integer> acceptTogether(Acceptance first, Acceptance second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            results.add(submitAcceptance(executor, ready, start, first));
            results.add(submitAcceptance(executor, ready, start, second));
            assertTrue(ready.await(CONCURRENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS), "Acceptance threads did not start");
            start.countDown();
            long deadline = System.nanoTime() + Duration.ofSeconds(CONCURRENCY_TIMEOUT_SECONDS).toNanos();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                long remaining = deadline - System.nanoTime();
                assertTrue(remaining > 0, "Concurrent acceptances exceeded the timeout");
                statuses.add(result.get(remaining, TimeUnit.NANOSECONDS));
            }
            return statuses;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(CONCURRENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "Acceptance threads did not terminate");
        }
    }

    private Future<Integer> submitAcceptance(ExecutorService executor, CountDownLatch ready, CountDownLatch start,
            Acceptance acceptance) {
        return executor.submit(() -> {
            ready.countDown();
            if (!start.await(CONCURRENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Acceptance start signal timed out");
            }
            return mockMvc.perform(post(TRADES + "/" + acceptance.tradeId() + "/accept")
                            .header(HttpHeaders.AUTHORIZATION, acceptance.token()))
                    .andReturn().getResponse().getStatus();
        });
    }

    private Map<Long, Long> cardTotals() {
        return jdbcTemplate.query("SELECT card_id, SUM(quantity) FROM user_card GROUP BY card_id ORDER BY card_id",
                result -> {
                    Map<Long, Long> totals = new HashMap<>();
                    while (result.next()) {
                        totals.put(result.getLong(1), result.getLong(2));
                    }
                    return totals;
                });
    }

    private long userId(String nickname) {
        return jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE nickname = ?", Long.class, nickname);
    }

    private long cardId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM card WHERE code = ?", Long.class, code);
    }

    private void own(long userId, long cardId, int copies) {
        jdbcTemplate.update("INSERT INTO user_card (user_id, card_id, quantity) VALUES (?, ?, ?)",
                userId, cardId, copies);
    }

    private int quantity(long userId, long cardId) {
        Integer quantity = jdbcTemplate.query("SELECT quantity FROM user_card WHERE user_id = ? AND card_id = ?",
                result -> result.next() ? result.getInt(1) : null, userId, cardId);
        return quantity == null ? 0 : quantity;
    }

    private record Acceptance(String token, long tradeId) {
    }

    private record TestUser(String nickname, String token) {
    }
}
