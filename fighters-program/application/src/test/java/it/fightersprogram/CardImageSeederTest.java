package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import it.fightersprogram.catalog.CardImageSeeder;
import it.fightersprogram.catalog.domain.CardImageStorage;

@SpringBootTest
class CardImageSeederTest {

    private static final byte[] ADMIN_UPLOAD = "uploaded by an admin".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private CardImageStorage storage;

    @Autowired
    private CardImageSeeder seeder;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void everyCardHasItsIllustrationAfterStartup() {
        List<String> codes = jdbc.queryForList("SELECT code FROM card", String.class);

        assertFalse(codes.isEmpty());
        for (String code : codes) {
            assertTrue(storage.exists(code + ".webp"), code);
        }
    }

    @Test
    void rerunningTheSeederKeepsAnImageThatWasReplaced() throws Exception {
        storage.save("AF01.webp", ADMIN_UPLOAD);

        seeder.run(null);

        assertArrayEquals(ADMIN_UPLOAD,
                storage.load("AF01.webp").orElseThrow().resource().getContentAsByteArray());
    }
}
