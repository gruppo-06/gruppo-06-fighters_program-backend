package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.importer.CardEntry;
import it.fightersprogram.catalog.importer.CardStats;
import it.fightersprogram.catalog.importer.CatalogFile;
import it.fightersprogram.catalog.importer.CatalogImportException;
import it.fightersprogram.catalog.importer.CatalogImporter;
import it.fightersprogram.catalog.importer.ImportSummary;

@SpringBootTest
@Transactional
class CatalogImporterTest {

    @Autowired
    private CatalogImporter importer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertsNewCardsWithTheirReferences() {
        ImportSummary summary = importer.importAll(List.of(africanFile(entry("ZT01", "Assassini", "Leggendaria", 281))));

        assertEquals(new ImportSummary(1, 0), summary);
        Map<String, Object> card = jdbcTemplate.queryForMap("""
                SELECT c.name, c.country, f.name AS faction, fc.name AS fighter_class, r.name AS rarity,
                       c.atk, c.def, c.vel, c.crt, c.flavor_text, c.image_file
                FROM card c
                JOIN faction f ON f.id = c.faction_id
                JOIN fighter_class fc ON fc.id = c.class_id
                JOIN rarity r ON r.id = c.rarity_id
                WHERE c.code = 'ZT01'
                """);
        assertEquals("Fighter ZT01", card.get("name"));
        assertEquals("Kenya", card.get("country"));
        assertEquals("Africana", card.get("faction"));
        assertEquals("Assassini", card.get("fighter_class"));
        assertEquals("Leggendaria", card.get("rarity"));
        assertEquals(281, card.get("atk"));
        assertEquals(540, card.get("def"));
        assertEquals(850, card.get("vel"));
        assertEquals(400, card.get("crt"));
        assertEquals("Flavor ZT01", card.get("flavor_text"));
        assertEquals("ZT01.png", card.get("image_file"));
    }

    @Test
    void importingTwiceUpdatesInsteadOfDuplicating() {
        importer.importAll(List.of(africanFile(entry("ZT01", "Assassini", "Comune", 100))));

        ImportSummary second = importer.importAll(List.of(africanFile(entry("ZT01", "Colossi", "Rara", 150))));

        assertEquals(new ImportSummary(0, 1), second);
        assertEquals(1, countTestCards());
        assertEquals(150, jdbcTemplate.queryForObject("SELECT atk FROM card WHERE code = 'ZT01'", Integer.class));
    }

    @Test
    void unknownClassStopsTheImportBeforeWritingAnything() {
        CatalogFile file = africanFile(entry("ZT01", "Assassini", "Comune", 100), entry("ZT02", "Maghi", "Comune", 100));

        CatalogImportException exception =
                assertThrows(CatalogImportException.class, () -> importer.importAll(List.of(file)));

        assertTrue(exception.getMessage().contains("ZT02"), exception.getMessage());
        assertTrue(exception.getMessage().contains("Fighter ZT02"), exception.getMessage());
        assertTrue(exception.getMessage().contains("Maghi"), exception.getMessage());
        assertEquals(0, countTestCards());
    }

    @Test
    void unknownRarityStopsTheImport() {
        CatalogFile file = africanFile(entry("ZT01", "Assassini", "Mitica", 100));

        CatalogImportException exception =
                assertThrows(CatalogImportException.class, () -> importer.importAll(List.of(file)));

        assertTrue(exception.getMessage().contains("Mitica"), exception.getMessage());
    }

    @Test
    void unknownFactionStopsTheImport() {
        CatalogFile file = new CatalogFile(1, "Atlantidea", List.of(entry("ZT01", "Assassini", "Comune", 100)));

        CatalogImportException exception =
                assertThrows(CatalogImportException.class, () -> importer.importAll(List.of(file)));

        assertTrue(exception.getMessage().contains("Atlantidea"), exception.getMessage());
    }

    private int countTestCards() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM card WHERE code LIKE 'ZT%'", Integer.class);
    }

    private static CatalogFile africanFile(CardEntry... entries) {
        return new CatalogFile(1, "Africana", List.of(entries));
    }

    private static CardEntry entry(String code, String fighterClass, String rarity, int attack) {
        return new CardEntry(code, "Fighter " + code, "Kenya", fighterClass, rarity,
                new CardStats(attack, 540, 850, 400), "Flavor " + code, code + ".png");
    }
}
