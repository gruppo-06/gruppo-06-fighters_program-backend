package it.fightersprogram.catalog.importer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import it.fightersprogram.catalog.domain.Subcard;
import it.fightersprogram.catalog.domain.SubcardRepository;

import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = "catalog.import.enabled", havingValue = "true")
public class SubcardImportRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubcardImportRunner.class);
    private static final String LOCATION = "catalog/subcards/subcards.json";

    private final JsonMapper jsonMapper;
    private final SubcardRepository repository;
    private final TransactionTemplate transactionTemplate;

    public SubcardImportRunner(JsonMapper jsonMapper, SubcardRepository repository,
            TransactionTemplate transactionTemplate) {
        this.jsonMapper = jsonMapper;
        this.repository = repository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        SubcardFile file = read();
        int count = transactionTemplate.execute(status -> {
            file.subcards().forEach(this::upsert);
            return file.subcards().size();
        });
        LOGGER.info("Imported {} sub-cards from {}", count, LOCATION);
    }

    private void upsert(SubcardEntry entry) {
        Modifiers modifiers = entry.modifiers();
        int atk = orZero(modifiers.atk());
        int def = orZero(modifiers.def());
        int vel = orZero(modifiers.vel());
        int crt = orZero(modifiers.crt());
        repository.findByCode(entry.code())
                .ifPresentOrElse(
                        existing -> existing.update(entry.name(), entry.description(), atk, def, vel, crt),
                        () -> repository.save(new Subcard(entry.code(), entry.name(), entry.description(),
                                atk, def, vel, crt)));
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private SubcardFile read() {
        ClassPathResource resource = new ClassPathResource(LOCATION);
        try (InputStream json = resource.getInputStream()) {
            return jsonMapper.readValue(json, SubcardFile.class);
        } catch (IOException exception) {
            throw new CatalogImportException("Cannot open sub-card catalog " + LOCATION, exception);
        }
    }

    public record SubcardFile(int schemaVersion, List<SubcardEntry> subcards) {

        public SubcardFile {
            subcards = subcards == null ? List.of() : List.copyOf(subcards);
        }
    }

    public record SubcardEntry(String code, String name, String description, Modifiers modifiers) {
    }

    public record Modifiers(Integer atk, Integer def, Integer vel, Integer crt) {
    }
}
