package it.fightersprogram;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import it.fightersprogram.economy.CreditService;
import it.fightersprogram.economy.CreditType;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PackOpeningApiTest {

    private static final String KEY_HEADER = "Idempotency-Key";
    private static final String BASE_PACK = "Pacchetto Base";
    private static final int BASE_PRICE = 100;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CreditService creditService;

    @Test
    void openingTheBasePackDrawsFourCommonsAndOneRareAndDebitsThePrice() throws Exception {
        String token = TestUsers.registerAndLogin(mockMvc, "pack_base");
        long userId = userId("pack_base");
        creditService.credit(userId, 1_000, CreditType.ADMIN_GRANT, "pack-credit:" + UUID.randomUUID());
        long balanceBefore = creditService.balance(userId);
        long packId = packId(BASE_PACK);
        int commonsBefore = quantityOfRarity(userId, "Comune");
        int raresBefore = quantityOfRarity(userId, "Rara");

        open(token, packId, "base-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingId").isNumber())
                .andExpect(jsonPath("$.packId", is((int) packId)))
                .andExpect(jsonPath("$.packName", is(BASE_PACK)))
                .andExpect(jsonPath("$.price", is(BASE_PRICE)))
                .andExpect(jsonPath("$.cards", hasSize(5)))
                .andExpect(jsonPath("$.cards[?(@.rarity == 'Comune')]", hasSize(4)))
                .andExpect(jsonPath("$.cards[?(@.rarity == 'Rara')]", hasSize(1)))
                .andExpect(jsonPath("$.balance", is((int) (balanceBefore - BASE_PRICE))));

        assertEquals(balanceBefore - BASE_PRICE, creditService.balance(userId));
        assertEquals(1, packOpeningCount(userId));
        assertEquals(1, packPurchaseCount(userId));
        assertEquals(commonsBefore + 4, quantityOfRarity(userId, "Comune"));
        assertEquals(raresBefore + 1, quantityOfRarity(userId, "Rara"));
    }

    @Test
    void repeatingTheSameKeyReturnsTheSameOpeningAndDebitsOnlyOnce() throws Exception {
        String token = TestUsers.registerAndLogin(mockMvc, "pack_idempotent");
        long userId = userId("pack_idempotent");
        creditService.credit(userId, 1_000, CreditType.ADMIN_GRANT, "pack-credit:" + UUID.randomUUID());
        long balanceBefore = creditService.balance(userId);
        long packId = packId(BASE_PACK);

        String firstBody = open(token, packId, "same-key").andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        long firstOpeningId = ((Number) JsonPath.read(firstBody, "$.openingId")).longValue();
        List<Map<String, Object>> firstCards = JsonPath.read(firstBody, "$.cards");

        String secondBody = open(token, packId, "same-key")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is((int) (balanceBefore - BASE_PRICE))))
                .andReturn().getResponse().getContentAsString();
        assertEquals(firstOpeningId, ((Number) JsonPath.read(secondBody, "$.openingId")).longValue());
        assertEquals(firstCards, JsonPath.read(secondBody, "$.cards"));
        assertEquals(balanceBefore - BASE_PRICE, creditService.balance(userId));
        assertEquals(1, packOpeningCount(userId));
        assertEquals(1, packPurchaseCount(userId));
    }

    @Test
    void anInsufficientBalanceIsRefusedWithoutMovingAnything() throws Exception {
        String token = TestUsers.registerAndLogin(mockMvc, "pack_poor");
        long userId = userId("pack_poor");
        long balance = creditService.balance(userId);
        if (balance > 50) {
            creditService.debit(userId, (int) (balance - 50), CreditType.ADMIN_GRANT,
                    "pack-drain:" + UUID.randomUUID());
        }
        long balanceBefore = creditService.balance(userId);
        assertTrue(balanceBefore < BASE_PRICE);
        long packId = packId(BASE_PACK);
        int commonsBefore = quantityOfRarity(userId, "Comune");
        int raresBefore = quantityOfRarity(userId, "Rara");

        open(token, packId, "too-poor")
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_CREDITS")))
                .andExpect(jsonPath("$.details", hasSize(0)));

        assertEquals(balanceBefore, creditService.balance(userId));
        assertEquals(0, packOpeningCount(userId));
        assertEquals(0, packPurchaseCount(userId));
        assertEquals(commonsBefore, quantityOfRarity(userId, "Comune"));
        assertEquals(raresBefore, quantityOfRarity(userId, "Rara"));
    }

    @Test
    void aMissingOrBlankIdempotencyKeyIsRejected() throws Exception {
        String token = TestUsers.registerAndLogin(mockMvc, "pack_header");
        long packId = packId(BASE_PACK);

        mockMvc.perform(post("/api/packs/{id}/open", packId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
        mockMvc.perform(post("/api/packs/{id}/open", packId).header(HttpHeaders.AUTHORIZATION, token)
                        .header(KEY_HEADER, "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    void anUnknownOrInactivePackIsNotFound() throws Exception {
        String token = TestUsers.registerAndLogin(mockMvc, "pack_missing");
        long inactivePackId = jdbcTemplate.queryForObject("""
                INSERT INTO pack (name, description, price, active)
                VALUES (?, 'inactive test pack', 100, FALSE)
                RETURNING id
                """, Long.class, "Inactive " + UUID.randomUUID());

        open(token, 999_999L, "unknown").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        open(token, inactivePackId, "inactive").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void aPackWithoutCandidatesRollsBackTheDebitTheCardsAndTheOpening() throws Exception {
        String nickname = "pr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String token = TestUsers.registerAndLogin(mockMvc, nickname);
        long userId = userId(nickname);
        creditService.credit(userId, 1_000, CreditType.ADMIN_GRANT, "pack-credit:" + UUID.randomUUID());
        long balanceBefore = creditService.balance(userId);
        long packId = packId(BASE_PACK);
        int commonsBefore = quantityOfRarity(userId, "Comune");
        int raresBefore = quantityOfRarity(userId, "Rara");
        long openingsBefore = packOpeningCount(userId);

        try {
            jdbcTemplate.update(
                    "UPDATE card SET active = FALSE WHERE rarity_id = (SELECT id FROM rarity WHERE name = 'Rara')");
            entityManager.clear();

            open(token, packId, "rollback")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code", is("PACK_UNAVAILABLE")))
                    .andExpect(jsonPath("$.details", hasSize(0)));

            assertEquals(balanceBefore, creditService.balance(userId));
            assertEquals(openingsBefore, packOpeningCount(userId));
            assertEquals(commonsBefore, quantityOfRarity(userId, "Comune"));
            assertEquals(raresBefore, quantityOfRarity(userId, "Rara"));
            assertEquals(0, packPurchaseCount(userId));
        } finally {
            jdbcTemplate.update(
                    "UPDATE card SET active = TRUE WHERE rarity_id = (SELECT id FROM rarity WHERE name = 'Rara')");
        }
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/packs/{id}/open", packId(BASE_PACK)).header(KEY_HEADER, "anonymous"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions open(String token, long packId, String key) throws Exception {
        return mockMvc.perform(post("/api/packs/{id}/open", packId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .header(KEY_HEADER, key));
    }

    private long userId(String nickname) {
        return jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE nickname = ?", Long.class, nickname);
    }

    private long packId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM pack WHERE name = ?", Long.class, name);
    }

    private int quantityOfRarity(long userId, String rarity) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(uc.quantity), 0)
                FROM user_card uc
                JOIN card c ON c.id = uc.card_id
                JOIN rarity r ON r.id = c.rarity_id
                WHERE uc.user_id = ? AND r.name = ?
                """, Integer.class, userId, rarity);
    }

    private int packOpeningCount(long userId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pack_opening WHERE user_id = ?", Integer.class,
                userId);
    }

    private int packPurchaseCount(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credit_transaction WHERE user_id = ? AND type = 'PACK_PURCHASE'",
                Integer.class, userId);
    }
}
