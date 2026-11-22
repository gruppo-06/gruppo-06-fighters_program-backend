package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.economy.domain.Pack;
import it.fightersprogram.economy.domain.PackRepository;

@SpringBootTest
@Transactional
class PackSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PackRepository packRepository;

    @Test
    void seedPacksExistWithCorrectPricesAndContents() {
        // Verify pack names and prices
        List<Map<String, Object>> packs = jdbcTemplate.queryForList("""
                SELECT p.name, p.price, r.name AS rarity, pc.card_count
                FROM pack p
                JOIN pack_content pc ON pc.pack_id = p.id
                JOIN rarity r ON r.id = pc.rarity_id
                ORDER BY p.price, r.rank
                """);

        assertEquals(9, packs.size(), "Expected 9 pack_content rows across 3 packs");

        // Pacchetto Base: price 100, Comune 4, Rara 1
        assertPackRow(packs, 0, "Pacchetto Base", 100, "Comune", 4);
        assertPackRow(packs, 1, "Pacchetto Base", 100, "Rara", 1);

        // Pacchetto Avanzato: price 220, Comune 5, Rara 2, Epica 1
        assertPackRow(packs, 2, "Pacchetto Avanzato", 220, "Comune", 5);
        assertPackRow(packs, 3, "Pacchetto Avanzato", 220, "Rara", 2);
        assertPackRow(packs, 4, "Pacchetto Avanzato", 220, "Epica", 1);

        // Pacchetto Leggendario: price 500, Comune 6, Rara 2, Epica 1, Leggendaria 1
        assertPackRow(packs, 5, "Pacchetto Leggendario", 500, "Comune", 6);
        assertPackRow(packs, 6, "Pacchetto Leggendario", 500, "Rara", 2);
        assertPackRow(packs, 7, "Pacchetto Leggendario", 500, "Epica", 1);
        assertPackRow(packs, 8, "Pacchetto Leggendario", 500, "Leggendaria", 1);
    }

    @Test
    void rejectsNegativePrice() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO pack (name, description, price) VALUES ('Bad Pack', 'desc', -10)
                """));
    }

    @Test
    void rejectsZeroCardCount() {
        long packId = jdbcTemplate.queryForObject("""
                INSERT INTO pack (name, description, price) VALUES ('Test Pack', 'desc', 100)
                RETURNING id
                """, Long.class);

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO pack_content (pack_id, rarity_id, card_count) VALUES (?, 1, 0)
                """, packId));
    }

    @Test
    void rejectsReusedIdempotencyKeyInPackOpening() {
        long userId = insertUser("test_user");
        long packId = jdbcTemplate.queryForObject("""
                INSERT INTO pack (name, description, price) VALUES ('Test Pack', 'desc', 100)
                RETURNING id
                """, Long.class);

        String cardsJson = "[{\"cardId\": 1, \"quantity\": 1}]";
        String idempotencyKey = "unique-key-123";

        assertDoesNotThrow(() -> jdbcTemplate.update("""
                INSERT INTO pack_opening (user_id, pack_id, cards, idempotency_key)
                VALUES (?, ?, CAST(? AS jsonb), ?)
                """, userId, packId, cardsJson, idempotencyKey));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO pack_opening (user_id, pack_id, cards, idempotency_key)
                VALUES (?, ?, CAST(? AS jsonb), ?)
                """, userId, packId, cardsJson, idempotencyKey));
    }

    @Test
    void packRepositoryReturnsActivePacksByPriceAndReadsContents() {
        // Clear and create test packs for this test
        jdbcTemplate.update("DELETE FROM pack_content");
        jdbcTemplate.update("DELETE FROM pack");

        jdbcTemplate.update("""
                INSERT INTO pack (name, description, price) VALUES
                ('Test Pack 1', 'desc', 100),
                ('Test Pack 2', 'desc', 200),
                ('Test Pack 3', 'desc', 300)
                """);
        jdbcTemplate.update("""
                INSERT INTO pack_content (pack_id, rarity_id, card_count) VALUES
                ((SELECT id FROM pack WHERE name = 'Test Pack 1'), 1, 4),
                ((SELECT id FROM pack WHERE name = 'Test Pack 1'), 2, 1),
                ((SELECT id FROM pack WHERE name = 'Test Pack 2'), 1, 5),
                ((SELECT id FROM pack WHERE name = 'Test Pack 2'), 2, 2),
                ((SELECT id FROM pack WHERE name = 'Test Pack 2'), 3, 1),
                ((SELECT id FROM pack WHERE name = 'Test Pack 3'), 1, 6),
                ((SELECT id FROM pack WHERE name = 'Test Pack 3'), 2, 2),
                ((SELECT id FROM pack WHERE name = 'Test Pack 3'), 3, 1),
                ((SELECT id FROM pack WHERE name = 'Test Pack 3'), 4, 1)
                """);

        // Initially all 3 test packs are active
        List<Pack> activePacks = packRepository.findAllByActiveTrueOrderByPriceAsc();
        assertEquals(3, activePacks.size());

        assertEquals("Test Pack 1", activePacks.get(0).getName());
        assertEquals(100, activePacks.get(0).getPrice());
        assertEquals(5, activePacks.get(0).totalCards());
        assertEquals(2, activePacks.get(0).getContents().size());

        assertEquals("Test Pack 2", activePacks.get(1).getName());
        assertEquals(200, activePacks.get(1).getPrice());
        assertEquals(8, activePacks.get(1).totalCards());
        assertEquals(3, activePacks.get(1).getContents().size());

        assertEquals("Test Pack 3", activePacks.get(2).getName());
        assertEquals(300, activePacks.get(2).getPrice());
        assertEquals(10, activePacks.get(2).totalCards());
        assertEquals(4, activePacks.get(2).getContents().size());

        // Deactivate one pack and verify it's filtered out
        Pack basePack = activePacks.get(0);
        basePack.deactivate();
        packRepository.saveAndFlush(basePack);

        activePacks = packRepository.findAllByActiveTrueOrderByPriceAsc();
        assertEquals(2, activePacks.size());
        assertEquals("Test Pack 2", activePacks.get(0).getName());
        assertEquals("Test Pack 3", activePacks.get(1).getName());

        // findByIdAndActiveTrue
        assertTrue(packRepository.findByIdAndActiveTrue(activePacks.get(0).getId()).isPresent());
        assertTrue(packRepository.findByIdAndActiveTrue(basePack.getId()).isEmpty());
    }

    private void assertPackRow(List<Map<String, Object>> packs, int index,
                               String packName, int price, String rarity, int cardCount) {
        Map<String, Object> row = packs.get(index);
        assertEquals(packName, row.get("name"));
        assertEquals(price, row.get("price"));
        assertEquals(rarity, row.get("rarity"));
        assertEquals(cardCount, row.get("card_count"));
    }

    private long insertUser(String nickname) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO app_user (nickname, email, password_hash)
                VALUES (?, ?, 'hash')
                RETURNING id
                """, Long.class, nickname, nickname + "@example.com");
    }
}