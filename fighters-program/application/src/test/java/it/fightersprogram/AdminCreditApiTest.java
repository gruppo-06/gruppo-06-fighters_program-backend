package it.fightersprogram;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminCreditApiTest {

    private static final String PLAYER = "grant_player";
    private static final String KEY_HEADER = "Idempotency-Key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private String admin;
    private String player;

    @BeforeEach
    void logIn() throws Exception {
        admin = TestUsers.registerAndLoginAsAdmin(mockMvc, entityManager, "grant_admin");
        player = TestUsers.registerAndLogin(mockMvc, PLAYER);
    }

    @Test
    void aGrantShowsInThePlayersCredits() throws Exception {
        long balanceBefore = balanceOf(PLAYER);

        grant(admin, PLAYER, grantJson(50, "Tournament prize"), "grant-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname", is(PLAYER)))
                .andExpect(jsonPath("$.balance", is((int) balanceBefore + 50)))
                .andExpect(jsonPath("$.movement.amount", is(50)))
                .andExpect(jsonPath("$.movement.type", is("ADMIN_GRANT")))
                .andExpect(jsonPath("$.movement.reference", is("Tournament prize (by grant_admin)")));

        mockMvc.perform(get("/api/me/credits").header(HttpHeaders.AUTHORIZATION, player))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is((int) balanceBefore + 50)))
                .andExpect(jsonPath("$.movements[0].amount", is(50)))
                .andExpect(jsonPath("$.movements[0].type", is("ADMIN_GRANT")))
                .andExpect(jsonPath("$.movements[0].reference", is("Tournament prize (by grant_admin)")));
    }

    @Test
    void aNegativeGrantDebitsThePlayer() throws Exception {
        grant(admin, PLAYER, grantJson(100, "Prize"), "grant-credit").andExpect(status().isCreated());
        long balanceBefore = balanceOf(PLAYER);

        grant(admin, PLAYER, grantJson(-30, "Correction"), "grant-debit")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance", is((int) balanceBefore - 30)))
                .andExpect(jsonPath("$.movement.amount", is(-30)))
                .andExpect(jsonPath("$.movement.type", is("ADMIN_GRANT")));
    }

    @Test
    void theSameIdempotencyKeyAppliesOnlyOnce() throws Exception {
        grant(admin, PLAYER, grantJson(40, "Prize"), "same-key").andExpect(status().isCreated());
        long balanceAfterFirst = balanceOf(PLAYER);

        grant(admin, PLAYER, grantJson(40, "Prize"), "same-key")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance", is((int) balanceAfterFirst)));

        assertEquals(1, grantCount("admin-grant:same-key"));
        assertEquals(balanceAfterFirst, balanceOf(PLAYER));
    }

    @Test
    void aNegativeGrantBeyondTheBalanceIsAConflictWithoutAMovement() throws Exception {
        grant(admin, PLAYER, grantJson(10, "Prize"), "small-grant").andExpect(status().isCreated());
        long balanceBefore = balanceOf(PLAYER);

        grant(admin, PLAYER, grantJson(-(int) balanceBefore - 1, "Too much"), "overdraft")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));

        assertEquals(0, grantCount("admin-grant:overdraft"));
        assertEquals(balanceBefore, balanceOf(PLAYER));
    }

    @Test
    void aZeroAmountIsRejected() throws Exception {
        grant(admin, PLAYER, grantJson(0, "Nothing"), "zero")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("zero")));
    }

    @Test
    void anAmountOutOfRangeIsRejected() throws Exception {
        grant(admin, PLAYER, grantJson(100_001, "Too big"), "too-big")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        grant(admin, PLAYER, grantJson(-100_001, "Too small"), "too-small")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        grant(admin, PLAYER, grantJson(null, "Missing"), "missing-amount")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void aBlankReasonIsRejected() throws Exception {
        grant(admin, PLAYER, grantJson(10, "   "), "blank-reason")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.details", hasItem("reason: must not be blank")));
    }

    @Test
    void aMissingOrBlankIdempotencyKeyIsRejected() throws Exception {
        mockMvc.perform(post(grantsOf(PLAYER)).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(grantJson(10, "Prize")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));

        grant(admin, PLAYER, grantJson(10, "Prize"), "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    void anUnknownNicknameIsNotFound() throws Exception {
        grant(admin, "nobody_here", grantJson(10, "Prize"), "unknown")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    void aPlayerCannotGrantCredits() throws Exception {
        grant(player, PLAYER, grantJson(10, "Prize"), "player-grant").andExpect(status().isForbidden());
        assertEquals(0, grantCount("admin-grant:player-grant"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post(grantsOf(PLAYER))
                .header(KEY_HEADER, "anonymous")
                .contentType(MediaType.APPLICATION_JSON)
                .content(grantJson(10, "Prize")))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions grant(String token, String nickname, String body, String key) throws Exception {
        return mockMvc.perform(post(grantsOf(nickname))
                .header(HttpHeaders.AUTHORIZATION, token)
                .header(KEY_HEADER, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long balanceOf(String nickname) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(movement.amount), 0) FROM credit_transaction movement "
                        + "JOIN app_user owner ON owner.id = movement.user_id WHERE owner.nickname = ?",
                Long.class, nickname);
    }

    private int grantCount(String idempotencyKey) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credit_transaction WHERE idempotency_key = ? AND type = 'ADMIN_GRANT'",
                Integer.class, idempotencyKey);
    }

    private static String grantsOf(String nickname) {
        return "/api/admin/users/" + nickname + "/credits";
    }

    private static String grantJson(Integer amount, String reason) {
        return """
                {"amount": %s, "reason": "%s"}
                """.formatted(amount == null ? "null" : amount, reason);
    }
}
