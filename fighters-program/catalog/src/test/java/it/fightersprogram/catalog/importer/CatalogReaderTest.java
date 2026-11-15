package it.fightersprogram.catalog.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class CatalogReaderTest {

    private static final String FIXTURE = "/catalog-fixtures/catalogo-euroatlantica.json";

    private final CatalogReader reader = new CatalogReader(JsonMapper.builder().build());

    @Test
    void readsEveryCardOfAFactionFile() throws IOException {
        try (InputStream json = getClass().getResourceAsStream(FIXTURE)) {
            CatalogFile file = reader.read(json, FIXTURE);

            assertEquals(1, file.schemaVersion());
            assertEquals("Euroatlantica", file.faction());
            assertEquals(16, file.cards().size());
        }
    }

    @Test
    void mapsEveryFieldOfACard() throws IOException {
        try (InputStream json = getClass().getResourceAsStream(FIXTURE)) {
            CardEntry hana = reader.read(json, FIXTURE).cards().getLast();

            assertEquals(new CardEntry("EA16", "Hana Kuroda", "Giappone", "Assassini", "Leggendaria",
                    new CardStats(281, 540, 850, 400), "Fixture flavor text.", "EA16.png"), hana);
        }
    }

    @Test
    void readsEveryFileMatchingALocationPattern() {
        List<CatalogFile> files = reader.readAll("classpath:catalog-fixtures/*.json");

        assertEquals(1, files.size());
    }

    @Test
    void findsNoFileWhenTheLocationIsEmpty() {
        assertTrue(reader.readAll("classpath:missing-catalog/*.json").isEmpty());
    }

    @Test
    void reportsTheFileNameWhenTheJsonIsMalformed() {
        InputStream broken = new ByteArrayInputStream("{ \"faction\": ".getBytes(StandardCharsets.UTF_8));

        CatalogImportException exception =
                assertThrows(CatalogImportException.class, () -> reader.read(broken, "catalogo-rotto.json"));

        assertTrue(exception.getMessage().contains("catalogo-rotto.json"));
    }
}
