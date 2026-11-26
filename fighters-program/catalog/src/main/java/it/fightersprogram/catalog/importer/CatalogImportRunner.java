package it.fightersprogram.catalog.importer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "catalog.import.enabled", havingValue = "true")
@Order(0)
public class CatalogImportRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogImportRunner.class);

    private final CatalogReader reader;
    private final CatalogValidator validator;
    private final CatalogImporter importer;
    private final String location;

    public CatalogImportRunner(CatalogReader reader, CatalogValidator validator, CatalogImporter importer,
            @Value("${catalog.import.location}") String location) {
        this.reader = reader;
        this.validator = validator;
        this.importer = importer;
        this.location = location;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        List<CatalogFile> files = reader.readAll(location);
        if (files.isEmpty()) {
            LOGGER.warn("No catalog file found at {}: the card catalog was not imported", location);
            return;
        }
        validator.validate(files);
        ImportSummary summary = importer.importAll(files);
        LOGGER.info("Imported {} catalog files from {}: {} cards created, {} updated, {} skipped as admin-managed",
                files.size(), location, summary.created(), summary.updated(), summary.skipped());
    }
}
