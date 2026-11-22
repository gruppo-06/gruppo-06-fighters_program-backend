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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import it.fightersprogram.economy.CreditService;
import it.fightersprogram.economy.CreditType;
import it.fightersprogram.economy.InsufficientCreditsException;

@SpringBootTest
class CreditConcurrencyTest {

    private static final int TIMEOUT_SECONDS = 5;

    @Autowired
    private CreditService creditService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long userId;

    @BeforeEach
    void insertUser() {
        String nickname = "cc" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        userId = jdbcTemplate.queryForObject("""
                INSERT INTO app_user (nickname, email, password_hash) VALUES (?, ?, 'hash') RETURNING id
                """, Long.class, nickname, nickname + "@example.com");
    }

    @RepeatedTest(10)
    void twoSimultaneousDebitsNeverTakeTheBalanceBelowZero() throws Exception {
        creditService.credit(userId, 100, CreditType.WELCOME, "welcome:" + userId);

        List<String> outcomes = runTogether(
                () -> debit(60, "pack:" + userId + ":1"),
                () -> debit(60, "pack:" + userId + ":2"));

        assertEquals(1, outcomes.stream().filter("debited"::equals).count(), outcomes.toString());
        assertEquals(1, outcomes.stream().filter("insufficient"::equals).count(), outcomes.toString());
        assertEquals(40, creditService.balance(userId));
    }

    @RepeatedTest(10)
    void theSameKeySentTwiceAtOnceCreditsOnlyOnce() throws Exception {
        String key = "daily:" + userId;

        runTogether(
                () -> String.valueOf(creditService.credit(userId, 30, CreditType.DAILY, key).id()),
                () -> String.valueOf(creditService.credit(userId, 30, CreditType.DAILY, key).id()));

        assertEquals(30, creditService.balance(userId));
    }

    private String debit(int amount, String key) {
        try {
            creditService.debit(userId, amount, CreditType.PACK_PURCHASE, key);
            return "debited";
        } catch (InsufficientCreditsException exception) {
            return "insufficient";
        }
    }

    private List<String> runTogether(Callable<String> first, Callable<String> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (Callable<String> task : List.of(first, second)) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<String> outcomes = new ArrayList<>();
            for (Future<String> future : futures) {
                outcomes.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
    }
}
