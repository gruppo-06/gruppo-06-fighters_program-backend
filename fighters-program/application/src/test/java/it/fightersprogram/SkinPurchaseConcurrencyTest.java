package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class SkinPurchaseConcurrencyTest {

    private static final int TIMEOUT_SECONDS = 10;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @RepeatedTest(10)
    void concurrentPurchasesChargeOnceAndCreateOneOwnershipRow() throws Exception {
        String nickname = "sc" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String token = TestUsers.registerAndLogin(mockMvc, nickname);
        long userId = jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE nickname = ?", Long.class, nickname);
        long skinId = jdbcTemplate.queryForObject("SELECT id FROM skin WHERE code = 'SK_AV_003'", Long.class);
        long initialBalance = jdbcTemplate.queryForObject("""
                SELECT COALESCE(sum(amount), 0) FROM credit_transaction WHERE user_id = ?
                """, Long.class, userId);

        List<Integer> statuses = runTogether(
                () -> purchaseStatus(token, skinId),
                () -> purchaseStatus(token, skinId));

        assertTrue(statuses.stream().allMatch(status -> status == 201 || status == 409), statuses.toString());
        assertTrue(statuses.contains(201), statuses.toString());
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT count(*) FROM credit_transaction WHERE user_id = ? AND type = 'SKIN_PURCHASE'
                """, Integer.class, userId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_skin WHERE user_id = ? AND skin_id = ?", Integer.class, userId, skinId));
        assertEquals(initialBalance - 100, jdbcTemplate.queryForObject("""
                SELECT COALESCE(sum(amount), 0) FROM credit_transaction WHERE user_id = ?
                """, Long.class, userId));
    }

    private int purchaseStatus(String token, long skinId) throws Exception {
        return mockMvc.perform(post("/api/skins/{id}/purchase", skinId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andReturn().getResponse().getStatus();
    }

    private List<Integer> runTogether(Callable<Integer> first, Callable<Integer> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> task : List.of(first, second)) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }
}
