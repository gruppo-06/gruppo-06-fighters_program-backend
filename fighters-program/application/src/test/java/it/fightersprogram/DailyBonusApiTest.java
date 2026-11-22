package it.fightersprogram;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.config.ClockConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(DailyBonusApiTest.SharedClockConfig.class)
class DailyBonusApiTest {

    private static final String DAILY = "/api/me/credits/daily";
    private static final String NICKNAME = "daily_api";
    private static final Instant START = Instant.parse("2026-11-22T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final MutableClock clock = SharedClockConfig.CLOCK;

    private String token;

    @BeforeEach
    void registerAndStartOnADay() throws Exception {
        clock.setInstant(START);
        token = TestUsers.registerAndLogin(mockMvc, NICKNAME);
    }

    @Test
    void theFirstClaimGivesTheFirstRewardAndRecordsAMovement() throws Exception {
        mockMvc.perform(post(DAILY).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(10)))
                .andExpect(jsonPath("$.streak", is(1)))
                .andExpect(jsonPath("$.nextClaimDate", is("2026-11-23")));

        assertEquals(10, dailyMovementAmount());
    }

    @Test
    void claimingOnTheNextDayIncreasesTheStreak() throws Exception {
        claim();
        clock.advance(Duration.ofDays(1));

        mockMvc.perform(post(DAILY).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(15)))
                .andExpect(jsonPath("$.streak", is(2)));
    }

    @Test
    void skippingADayResetsTheStreakToOne() throws Exception {
        claim();
        clock.advance(Duration.ofDays(1));
        claim();
        clock.advance(Duration.ofDays(2));

        mockMvc.perform(post(DAILY).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(10)))
                .andExpect(jsonPath("$.streak", is(1)));
    }

    @Test
    void aSecondClaimOnTheSameDayIsRejectedWithoutAnotherMovement() throws Exception {
        claim();

        mockMvc.perform(post(DAILY).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());

        assertEquals(1, dailyMovementCount());
    }

    @Test
    void theDayIsTheRomeDayNotTheUtcDay() throws Exception {
        clock.setInstant(Instant.parse("2026-11-21T23:30:00Z"));
        claim();
        clock.setInstant(Instant.parse("2026-11-22T12:00:00Z"));

        mockMvc.perform(post(DAILY).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());
    }

    @Test
    void theAmountStaysAtTheLastRewardAfterSevenDays() throws Exception {
        int[] expected = {10, 15, 20, 25, 30, 40, 50, 50, 50};
        for (int day = 0; day < expected.length; day++) {
            clock.setInstant(START.plus(Duration.ofDays(day)));
            mockMvc.perform(post(DAILY).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.amount", is(expected[day])))
                    .andExpect(jsonPath("$.streak", is(day + 1)));
        }
    }

    @Test
    void theStatusFollowsTheClaimsWithoutClaiming() throws Exception {
        assertStatus(true, 0, 10);
        assertStatus(true, 0, 10);
        assertEquals(0, dailyMovementCount());

        claim();
        assertStatus(false, 1, 15);

        clock.advance(Duration.ofDays(1));
        assertStatus(true, 1, 15);

        clock.advance(Duration.ofDays(2));
        assertStatus(true, 0, 10);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post(DAILY)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(DAILY)).andExpect(status().isUnauthorized());
    }

    private void claim() throws Exception {
        mockMvc.perform(post(DAILY).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
    }

    private void assertStatus(boolean claimableToday, int streak, int nextAmount) throws Exception {
        mockMvc.perform(get(DAILY).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimableToday", is(claimableToday)))
                .andExpect(jsonPath("$.streak", is(streak)))
                .andExpect(jsonPath("$.nextAmount", is(nextAmount)));
    }

    private long userId() {
        return jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE nickname = ?", Long.class, NICKNAME);
    }

    private int dailyMovementCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credit_transaction WHERE user_id = ? AND type = 'DAILY'", Integer.class,
                userId());
    }

    private int dailyMovementAmount() {
        return jdbcTemplate.queryForObject(
                "SELECT amount FROM credit_transaction WHERE user_id = ? AND type = 'DAILY'", Integer.class,
                userId());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SharedClockConfig {

        static final MutableClock CLOCK = new MutableClock(START, ClockConfig.GAME_ZONE);

        @Bean
        @Primary
        Clock testClock() {
            return CLOCK;
        }
    }
}

@SpringBootTest
@AutoConfigureMockMvc
@Import(DailyBonusApiTest.SharedClockConfig.class)
class DailyBonusConcurrencyTest {

    private static final String DAILY = "/api/me/credits/daily";
    private static final int TIMEOUT_SECONDS = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final MutableClock clock = DailyBonusApiTest.SharedClockConfig.CLOCK;

    @BeforeEach
    void startOnADay() {
        clock.setInstant(Instant.parse("2026-11-22T10:00:00Z"));
    }

    @RepeatedTest(10)
    void twoSimultaneousClaimsGiveOneSuccessAndOneConflictAndOneMovement() throws Exception {
        String nickname = "dc" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String token = TestUsers.registerAndLogin(mockMvc, nickname);

        List<Integer> statuses = runTogether(() -> claimStatus(token), () -> claimStatus(token));

        assertTrue(statuses.contains(200), statuses.toString());
        assertTrue(statuses.contains(409), statuses.toString());
        long userId = jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE nickname = ?", Long.class,
                nickname);
        int movements = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credit_transaction WHERE user_id = ? AND type = 'DAILY'", Integer.class,
                userId);
        assertEquals(1, movements);
    }

    private int claimStatus(String token) throws Exception {
        return mockMvc.perform(post(DAILY)
                .header(HttpHeaders.AUTHORIZATION, token)).andReturn().getResponse().getStatus();
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
