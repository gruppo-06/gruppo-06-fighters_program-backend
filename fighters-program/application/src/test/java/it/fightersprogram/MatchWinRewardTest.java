package it.fightersprogram;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import it.fightersprogram.economy.CreditService;
import it.fightersprogram.economy.CreditType;
import it.fightersprogram.matches.MatchFinished;
import it.fightersprogram.economy.MatchWinReward;

/**
 * Tests for {@link MatchWinReward}. Not {@code @Transactional} because the listener uses
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} which requires a real commit.
 */
@SpringBootTest(properties = "game.starter-collection.enabled=true")
@AutoConfigureMockMvc
class MatchWinRewardTest {

    private static final String MATCHES = "/api/matches";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CreditService creditService;

    @Autowired
    private MatchWinReward matchWinReward;

    @BeforeEach
    void cleanSlate() {
        jdbcTemplate.update("DELETE FROM credit_transaction");
        jdbcTemplate.update("DELETE FROM match");
        jdbcTemplate.update("DELETE FROM user_card");
        jdbcTemplate.update("DELETE FROM deck");
        jdbcTemplate.update("DELETE FROM app_user");
    }

    @Test
    void directListenerCallIsIdempotentForSameMatchFinished() {
        long userId = insertUser("mwr_user_" + randomSuffix());
        MatchFinished event = new MatchFinished(42L, userId);

        matchWinReward.onMatchFinished(event);
        matchWinReward.onMatchFinished(event);

        assertEquals(50, creditService.balance(userId));
        assertEquals(1, movementCount(userId));
    }

    @Test
    void drawCreatesNoMovement() {
        long userId = insertUser("mwr_user_" + randomSuffix());
        MatchFinished event = new MatchFinished(99L, null);

        matchWinReward.onMatchFinished(event);

        assertEquals(0, creditService.balance(userId));
        assertEquals(0, movementCount(userId));
    }

    @Test
    void endToEndWinnerGetsFiftyCredits() throws Exception {
        String aliceNickname = "mwr_alice_" + randomSuffix();
        String bobNickname = "mwr_bob_" + randomSuffix();

        String alice = registerAndLogin(aliceNickname);
        String bob = registerAndLogin(bobNickname);

        long aliceDeck = createDeck(alice);
        long bobDeck = createDeck(bob);

        long aliceId = userIdOf(aliceNickname);
        long bobId = userIdOf(bobNickname);

        long matchId = challenge(alice, bobNickname, aliceDeck)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()
                .transform(MatchWinRewardTest::idOf);

        long aliceBalanceBefore = creditService.balance(aliceId);
        long bobBalanceBefore = creditService.balance(bobId);

        accept(bob, matchId, bobDeck)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FINISHED")));

        long aliceBalanceAfter = creditService.balance(aliceId);
        long bobBalanceAfter = creditService.balance(bobId);

        Long winnerId = jdbcTemplate.queryForObject(
                "SELECT winner_id FROM match WHERE id = ?", Long.class, matchId);

        if (winnerId != null) {
            if (winnerId == aliceId) {
                assertEquals(50, aliceBalanceAfter - aliceBalanceBefore);
                assertEquals(0, bobBalanceAfter - bobBalanceBefore);
            } else {
                assertEquals(50, bobBalanceAfter - bobBalanceBefore);
                assertEquals(0, aliceBalanceAfter - aliceBalanceBefore);
            }
        } else {
            assertEquals(0, aliceBalanceAfter - aliceBalanceBefore);
            assertEquals(0, bobBalanceAfter - bobBalanceBefore);
        }
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private long insertUser(String nickname) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO app_user (nickname, email, password_hash) VALUES (?, ?, 'hash') RETURNING id
                """, Long.class, nickname, nickname + "@example.com");
    }

    private int movementCount(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM credit_transaction WHERE user_id = ?", Integer.class, userId);
    }

    private long userIdOf(String nickname) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE nickname = ?", Long.class, nickname);
    }

    private String registerAndLogin(String nickname) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nickname": "%s", "email": "%s", "password": "correct-horse-1"}
                        """.formatted(nickname, nickname + "@example.com")))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "correct-horse-1"}
                        """.formatted(nickname + "@example.com")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return "Bearer " + JsonPath.<String>read(body, "$.accessToken");
    }

    private long createDeck(String token) throws Exception {
        String body = mockMvc.perform(post("/api/me/decks")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestDecks.body("Main", TestDecks.starterDeck(jdbcTemplate))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    private ResultActions challenge(String token, String opponentNickname, long deckId) throws Exception {
        return mockMvc.perform(post(MATCHES)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"opponentNickname\": \"%s\", \"deckId\": %d}".formatted(opponentNickname, deckId)));
    }

    private ResultActions accept(String token, long matchId, long deckId) throws Exception {
        return mockMvc.perform(post(MATCHES + "/" + matchId + "/accept")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deckId\": %d}".formatted(deckId)));
    }

    private static long idOf(String body) {
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}