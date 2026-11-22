package it.fightersprogram.catalog;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import it.fightersprogram.catalog.domain.CardImageStorage;

@Component
public class CardImageSeeder implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CardImageSeeder.class);

    private final CardImageStorage storage;
    private final String seedLocation;
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public CardImageSeeder(CardImageStorage storage, @Value("${catalog.images.seed-location}") String seedLocation) {
        this.storage = storage;
        this.seedLocation = seedLocation;
    }

    @Override
    public void run(ApplicationArguments arguments) throws IOException {
        int copied = 0;
        for (Resource resource : resolver.getResources(seedLocation)) {
            String fileName = resource.getFilename();
            if (fileName == null || CardImageStorage.contentTypeOf(fileName).isEmpty() || storage.exists(fileName)) {
                continue;
            }
            storage.save(fileName, resource.getContentAsByteArray());
            copied++;
        }
        LOGGER.info("Copied {} bundled card illustrations into the image storage", copied);
    }
}
