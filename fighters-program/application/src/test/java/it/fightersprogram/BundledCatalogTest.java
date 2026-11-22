package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import it.fightersprogram.catalog.importer.CardEntry;
import it.fightersprogram.catalog.importer.CatalogFile;
import it.fightersprogram.catalog.importer.CatalogReader;
import it.fightersprogram.catalog.importer.CatalogValidator;
import tools.jackson.databind.json.JsonMapper;

class BundledCatalogTest {

    private static final String BUNDLED_CATALOG = "classpath*:catalog/*.json";
    private static final int FACTION_COUNT = 5;
    private static final String IMAGES = "catalog/images/";
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);

    private final List<CatalogFile> files = new CatalogReader(JsonMapper.builder().build()).readAll(BUNDLED_CATALOG);

    @Test
    void bundlesOneFileForEveryFaction() {
        assertEquals(FACTION_COUNT, files.size());
    }

    @Test
    void bundledFilesPassTheCatalogChecks() {
        assertDoesNotThrow(() -> new CatalogValidator().validate(files));
    }

    @Test
    void theLegendaryOfEveryFactionComesFromTheNewRoster() {
        Map<String, String> legendaryByFaction = files.stream().collect(Collectors.toMap(CatalogFile::faction,
                file -> file.cards().stream().filter(card -> "Leggendaria".equals(card.rarity()))
                        .map(CardEntry::name).findFirst().orElseThrow()));

        assertEquals(Map.of(
                "Africana", "Ama Mensah",
                "Eurasiatica", "Lev Morozov",
                "Euroatlantica", "Ren Saito",
                "Indo-Pacifica", "Khamla Vong",
                "Panamericana", "Valentina Rojas"), legendaryByFaction);
    }

    @Test
    void everyCardHasItsOwnWebpIllustrationNamedAfterItsCode() throws IOException {
        for (CatalogFile file : files) {
            for (CardEntry card : file.cards()) {
                assertEquals(card.id() + ".webp", card.image().file(), card.id());
                ClassPathResource image = new ClassPathResource(IMAGES + card.image().file());
                assertTrue(image.exists(), "Missing illustration for " + card.id());
                try (InputStream content = image.getInputStream()) {
                    byte[] header = content.readNBytes(12);
                    assertTrue(Arrays.equals(Arrays.copyOfRange(header, 0, 4), RIFF)
                            && Arrays.equals(Arrays.copyOfRange(header, 8, 12), WEBP),
                            card.id() + " is not a WebP image");
                }
            }
        }
    }
}
