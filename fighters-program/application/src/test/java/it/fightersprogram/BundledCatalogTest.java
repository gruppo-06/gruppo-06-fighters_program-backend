package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fightersprogram.catalog.importer.CatalogFile;
import it.fightersprogram.catalog.importer.CatalogReader;
import it.fightersprogram.catalog.importer.CatalogValidator;
import tools.jackson.databind.json.JsonMapper;

class BundledCatalogTest {

    private static final String BUNDLED_CATALOG = "classpath*:catalog/*.json";
    private static final int FACTION_COUNT = 5;

    private final List<CatalogFile> files = new CatalogReader(JsonMapper.builder().build()).readAll(BUNDLED_CATALOG);

    @Test
    void bundlesOneFileForEveryFaction() {
        assertEquals(FACTION_COUNT, files.size());
    }

    @Test
    void bundledFilesPassTheCatalogChecks() {
        assertDoesNotThrow(() -> new CatalogValidator().validate(files));
    }
}
