package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.cosmetics.importer.SkinCatalogReader;
import it.fightersprogram.cosmetics.importer.SkinImportException;
import it.fightersprogram.cosmetics.importer.SkinImporter;
import it.fightersprogram.cosmetics.importer.SkinEntry;

@SpringBootTest
@Transactional
class SkinCatalogImporterTest {

    @Autowired
    private SkinCatalogReader reader;

    @Autowired
    private SkinImporter importer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void importingBundledCatalogTwiceDoesNotDuplicateSkins() {
        List<SkinEntry> entries = reader.readAll("classpath*:cosmetics/*.json");

        importer.importAll(entries);
        importer.importAll(entries);

        assertEquals(10, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM skin WHERE code LIKE 'SK%'", Integer.class));
        assertEquals(10, jdbcTemplate.queryForObject(
                "SELECT count(DISTINCT code) FROM skin WHERE code LIKE 'SK%'", Integer.class));
        assertEquals(4, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM skin WHERE code LIKE 'SK%' AND type = 'AVATAR' AND card_id IS NULL",
                Integer.class));
        assertEquals(6, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM skin WHERE code LIKE 'SK%' AND type = 'CARD_VARIANT' AND card_id IS NOT NULL",
                Integer.class));
    }

    @Test
    void unknownCardCodeFailsWithAnActionableMessageBeforeWriting() {
        SkinEntry entry = new SkinEntry("SK_BAD_CARD", "Unknown card variant", "CARD_VARIANT",
                "NO-SUCH-CARD", "Comune", 100, "variants/unknown.png");

        SkinImportException exception = assertThrows(SkinImportException.class,
                () -> importer.importAll(List.of(entry)));

        assertTrue(exception.getMessage().contains("SK_BAD_CARD"), exception.getMessage());
        assertTrue(exception.getMessage().contains("NO-SUCH-CARD"), exception.getMessage());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM skin WHERE code = 'SK_BAD_CARD'", Integer.class));
    }

    @Test
    void unknownRarityFailsWithAnActionableMessage() {
        SkinEntry entry = new SkinEntry("SK_BAD_RARITY", "Unknown rarity avatar", "AVATAR",
                null, "Mitica", 100, "avatars/unknown.png");

        SkinImportException exception = assertThrows(SkinImportException.class,
                () -> importer.importAll(List.of(entry)));

        assertTrue(exception.getMessage().contains("SK_BAD_RARITY"), exception.getMessage());
        assertTrue(exception.getMessage().contains("Mitica"), exception.getMessage());
    }
}
