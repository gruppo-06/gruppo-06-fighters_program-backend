package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.economy.CreditService;
import it.fightersprogram.economy.CreditType;
import it.fightersprogram.cosmetics.SkinService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SkinPurchaseApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CreditService creditService;

    @Autowired
    private SkinService skinService;

    @Test
    void purchaseDebitsCreditsAndAddsSkinToTheAuthenticatedUsersCollection() throws Exception {
        String nickname = nickname();
        String token = TestUsers.registerAndLogin(mockMvc, nickname);
        long userId = userId(nickname);
        long balanceBefore = creditService.balance(userId);
        long skinId = skinId("SK_AV_003");
        assertTrue(balanceBefore >= 100);

        mockMvc.perform(post("/api/skins/" + skinId + "/purchase")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is((int) skinId)))
                .andExpect(jsonPath("$.code", is("SK_AV_003")))
                .andExpect(jsonPath("$.type", is("AVATAR")));

        assertEquals(balanceBefore - 100, creditService.balance(userId));
        mockMvc.perform(get("/api/me/skins").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].skin.code", is("SK_AV_003")))
                .andExpect(jsonPath("$[0].acquiredAt").isString());
        assertEquals(1, purchaseCount(userId));
    }

    @Test
    void alreadyOwnedSkinCannotBePurchasedAgain() throws Exception {
        String nickname = nickname();
        String token = TestUsers.registerAndLogin(mockMvc, nickname);
        long userId = userId(nickname);
        long skinId = skinId("SK_AV_003");
        long balanceBefore = creditService.balance(userId);

        mockMvc.perform(post("/api/skins/" + skinId + "/purchase")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/skins/" + skinId + "/purchase")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());

        assertEquals(balanceBefore - 100, creditService.balance(userId));
        assertEquals(1, ownedSkinCount(userId));
        assertEquals(1, purchaseCount(userId));
    }

    @Test
    void insufficientCreditsDoNotDebitOrGrantTheSkin() throws Exception {
        String nickname = nickname();
        String token = TestUsers.registerAndLogin(mockMvc, nickname);
        long userId = userId(nickname);
        long skinId = skinId("SK_AV_004");
        long balanceBefore = creditService.balance(userId);
        assertTrue(balanceBefore < 1000);

        mockMvc.perform(post("/api/skins/" + skinId + "/purchase")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Not enough credits")));

        assertEquals(balanceBefore, creditService.balance(userId));
        assertEquals(0, ownedSkinCount(userId));
        assertEquals(0, purchaseCount(userId));
    }

    @Test
    void cardVariantCanBePurchasedWithoutOwningItsCard() throws Exception {
        String nickname = nickname();
        String token = TestUsers.registerAndLogin(mockMvc, nickname);
        long userId = userId(nickname);
        long cardId = jdbcTemplate.queryForObject("SELECT id FROM card WHERE code = 'EA01'", Long.class);
        jdbcTemplate.update("DELETE FROM user_card WHERE user_id = ? AND card_id = ?", userId, cardId);
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_card WHERE user_id = ? AND card_id = ?", Integer.class, userId, cardId));
        long skinId = skinId("SK_CV_001");

        mockMvc.perform(post("/api/skins/" + skinId + "/purchase")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("SK_CV_001")))
                .andExpect(jsonPath("$.cardCode", is("EA01")));

        assertEquals(1, ownedSkinCount(userId));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_card WHERE user_id = ? AND card_id = ?", Integer.class, userId, cardId));
    }

    @Test
    void grantIsIdempotentAndDoesNotChargeTheUser() throws Exception {
        String nickname = nickname();
        String token = TestUsers.registerAndLogin(mockMvc, nickname);
        long userId = userId(nickname);
        long skinId = skinId("SK_AV_003");
        long balanceBefore = creditService.balance(userId);

        skinService.grant(userId, skinId, "BATTLE_PASS");
        skinService.grant(userId, skinId, "BATTLE_PASS");

        assertEquals(balanceBefore, creditService.balance(userId));
        assertEquals(1, ownedSkinCount(userId));
        assertEquals(0, purchaseCount(userId));
        mockMvc.perform(get("/api/me/skins").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].skin.code", is("SK_AV_003")));
    }

    @Test
    void purchaseRequiresAuthenticationAndUnknownOrInactiveSkinsAreNotFound() throws Exception {
        mockMvc.perform(post("/api/skins/999999/purchase")).andExpect(status().isUnauthorized());
        String nickname = nickname();
        String token = TestUsers.registerAndLogin(mockMvc, nickname);
        long skinId = skinId("SK_AV_003");

        mockMvc.perform(post("/api/skins/999999/purchase")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
        jdbcTemplate.update("UPDATE skin SET active = FALSE WHERE id = ?", skinId);
        mockMvc.perform(post("/api/skins/" + skinId + "/purchase")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownedSkinListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/me/skins")).andExpect(status().isUnauthorized());
    }

    private long userId(String nickname) {
        return jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE nickname = ?", Long.class, nickname);
    }

    private long skinId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM skin WHERE code = ?", Long.class, code);
    }

    private int ownedSkinCount(long userId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM user_skin WHERE user_id = ?", Integer.class, userId);
    }

    private int purchaseCount(long userId) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM credit_transaction WHERE user_id = ? AND type = 'SKIN_PURCHASE'
                """, Integer.class, userId);
    }

    private static String nickname() {
        return "sp" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
