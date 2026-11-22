package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.economy.CreditService;
import it.fightersprogram.economy.CreditType;
import it.fightersprogram.economy.PackOpeningResult;
import it.fightersprogram.economy.PackOpeningService;

@SpringBootTest
@Transactional
class PackDistributionTest {

    private static final String BASE_PACK = "Pacchetto Base";
    private static final String LEGENDARY_PACK = "Pacchetto Leggendario";
    private static final String COMMON = "Comune";
    private static final String RARE = "Rara";
    private static final String LEGENDARY = "Leggendaria";
    private static final int BASE_PRICE = 100;
    private static final int LEGENDARY_PRICE = 500;

    @Autowired
    private PackOpeningService packOpeningService;

    @Autowired
    private CreditService creditService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void basePackOpeningsDrawFourCommonsAndOneRareWithUniformOdds() {
        long userId = insertUser("pack_dist_base");
        creditService.credit(userId, 200_000, CreditType.ADMIN_GRANT, "pack-distribution:base-credit");
        long balanceBefore = creditService.balance(userId);
        long packId = packId(BASE_PACK);
        Map<Long, Integer> drawsByCard = new HashMap<>();

        for (int opening = 0; opening < 1000; opening++) {
            PackOpeningResult result = packOpeningService.open(userId, packId, "base-" + opening);
            List<String> rarities = result.cards().stream().map(card -> rarityOf(card.cardId())).toList();
            assertEquals(4, Collections.frequency(rarities, COMMON));
            assertEquals(1, Collections.frequency(rarities, RARE));
            result.cards().forEach(card -> drawsByCard.merge(card.cardId(), 1, Integer::sum));
        }

        assertEquals(balanceBefore - 1000L * BASE_PRICE, creditService.balance(userId));
        assertDrawsAreUniform(drawsByCard, COMMON, 4000);
        assertDrawsAreUniform(drawsByCard, RARE, 1000);
    }

    @Test
    void legendaryPackOpeningsDrawOneLegendaryCardEachTime() {
        long userId = insertUser("pack_dist_legend");
        creditService.credit(userId, 200 * LEGENDARY_PRICE, CreditType.ADMIN_GRANT,
                "pack-distribution:legendary-credit");
        long packId = packId(LEGENDARY_PACK);

        for (int opening = 0; opening < 200; opening++) {
            PackOpeningResult result = packOpeningService.open(userId, packId, "legendary-" + opening);
            List<String> rarities = result.cards().stream().map(card -> rarityOf(card.cardId())).toList();
            assertEquals(1, Collections.frequency(rarities, LEGENDARY));
        }
    }

    private void assertDrawsAreUniform(Map<Long, Integer> drawsByCard, String rarity, int totalDraws) {
        List<Long> activeCardIds = activeCardIds(rarity);
        double expected = (double) totalDraws / activeCardIds.size();
        for (long cardId : activeCardIds) {
            int draws = drawsByCard.getOrDefault(cardId, 0);
            assertTrue(draws >= expected * 0.5 && draws <= expected * 1.5,
                    () -> "Card " + cardId + " (" + rarity + ") drawn " + draws + " times, expected about " + expected);
        }
    }

    private List<Long> activeCardIds(String rarity) {
        return jdbcTemplate.queryForList("""
                SELECT c.id
                FROM card c
                JOIN rarity r ON r.id = c.rarity_id
                WHERE c.active AND r.name = ?
                """, Long.class, rarity);
    }

    private String rarityOf(long cardId) {
        return jdbcTemplate.queryForObject("""
                SELECT r.name
                FROM card c
                JOIN rarity r ON r.id = c.rarity_id
                WHERE c.id = ?
                """, String.class, cardId);
    }

    private long packId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM pack WHERE name = ?", Long.class, name);
    }

    private long insertUser(String nickname) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO app_user (nickname, email, password_hash)
                VALUES (?, ?, 'hash')
                RETURNING id
                """, Long.class, nickname, nickname + "@example.com");
    }
}
