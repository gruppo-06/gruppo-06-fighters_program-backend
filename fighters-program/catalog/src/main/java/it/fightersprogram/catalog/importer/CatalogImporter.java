package it.fightersprogram.catalog.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.domain.Card;
import it.fightersprogram.catalog.domain.CardDetails;
import it.fightersprogram.catalog.domain.CardRepository;
import it.fightersprogram.catalog.domain.Faction;
import it.fightersprogram.catalog.domain.FactionRepository;
import it.fightersprogram.catalog.domain.FighterClass;
import it.fightersprogram.catalog.domain.FighterClassRepository;
import it.fightersprogram.catalog.domain.Rarity;
import it.fightersprogram.catalog.domain.RarityRepository;

import lombok.RequiredArgsConstructor;

/**
 * Inserts or updates catalog cards by code, so running it again never duplicates them. Every reference name is
 * resolved before the first write: an unknown faction, class or rarity aborts the import with nothing written.
 * Cards an admin created or edited are skipped, and the active flag is never touched.
 */
@Service
@RequiredArgsConstructor
public class CatalogImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogImporter.class);

    private final CardRepository cardRepository;
    private final FactionRepository factionRepository;
    private final FighterClassRepository fighterClassRepository;
    private final RarityRepository rarityRepository;

    @Transactional
    public ImportSummary importAll(List<CatalogFile> files) {
        List<Map.Entry<String, CardDetails>> cards = resolveReferences(files);
        int created = 0;
        int updated = 0;
        int skipped = 0;
        for (Map.Entry<String, CardDetails> card : cards) {
            switch (upsert(card.getKey(), card.getValue())) {
                case CREATED -> created++;
                case UPDATED -> updated++;
                case SKIPPED -> skipped++;
            }
        }
        return new ImportSummary(created, updated, skipped);
    }

    private List<Map.Entry<String, CardDetails>> resolveReferences(List<CatalogFile> files) {
        List<Map.Entry<String, CardDetails>> cards = new ArrayList<>();
        for (CatalogFile file : files) {
            Faction faction = factionRepository.findByName(file.faction())
                    .orElseThrow(() -> new CatalogImportException("Unknown faction '" + file.faction() + "'"));
            for (CardEntry entry : file.cards()) {
                cards.add(Map.entry(entry.id(), toDetails(entry, faction)));
            }
        }
        return cards;
    }

    private CardDetails toDetails(CardEntry entry, Faction faction) {
        FighterClass fighterClass = fighterClassRepository.findByName(entry.fighterClass())
                .orElseThrow(() -> unknownReference(entry, "class", entry.fighterClass()));
        Rarity rarity = rarityRepository.findByName(entry.rarity())
                .orElseThrow(() -> unknownReference(entry, "rarity", entry.rarity()));
        CardStats stats = entry.stats();
        return new CardDetails(entry.name(), entry.country(), faction, fighterClass, rarity,
                stats.atk(), stats.def(), stats.vel(), stats.crt(), entry.copy(), entry.image().file());
    }

    private Outcome upsert(String code, CardDetails details) {
        return cardRepository.findByCode(code)
                .map(existing -> updateUnlessAdminManaged(existing, details))
                .orElseGet(() -> {
                    cardRepository.save(new Card(code, details));
                    return Outcome.CREATED;
                });
    }

    private static Outcome updateUnlessAdminManaged(Card existing, CardDetails details) {
        if (existing.isAdminManaged()) {
            LOGGER.info("Card {} is managed by an admin: the catalog file does not overwrite it", existing.getCode());
            return Outcome.SKIPPED;
        }
        existing.update(details);
        return Outcome.UPDATED;
    }

    private static CatalogImportException unknownReference(CardEntry entry, String field, String value) {
        return new CatalogImportException(
                "Card " + entry.id() + " (" + entry.name() + ") has unknown " + field + " '" + value + "'");
    }

    private enum Outcome {
        CREATED,
        UPDATED,
        SKIPPED
    }
}
