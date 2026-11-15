package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import it.fightersprogram.catalog.importer.CatalogImportRunner;

class CatalogImportRunnerTest {

    @Nested
    @SpringBootTest(properties = "catalog.import.enabled=true")
    class WhenEnabled {

        @Autowired
        private ApplicationContext context;

        @Test
        void registersTheStartupImport() {
            assertEquals(1, context.getBeanNamesForType(CatalogImportRunner.class).length);
        }
    }

    @Nested
    @SpringBootTest(properties = "catalog.import.enabled=false")
    class WhenDisabled {

        @Autowired
        private ApplicationContext context;

        @Test
        void skipsTheStartupImport() {
            assertEquals(0, context.getBeanNamesForType(CatalogImportRunner.class).length);
        }
    }
}
