package it.fightersprogram.cosmetics.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cosmetics.import.enabled", havingValue = "true")
@Order(1)
public class SkinCatalogImportRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkinCatalogImportRunner.class);

    private final SkinCatalogReader reader;
    private final SkinImporter importer;
    private final String location;

    public SkinCatalogImportRunner(SkinCatalogReader reader, SkinImporter importer,
            @Value("${cosmetics.import.location}") String location) {
        this.reader = reader;
        this.importer = importer;
        this.location = location;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        var skins = reader.readAll(location);
        importer.importAll(skins);
        LOGGER.info("Imported {} cosmetic skins from {}", skins.size(), location);
    }
}
