package it.fightersprogram.catalog.importer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Checks the DSG catalog files before anything is imported and reports every problem at once.
 */
@Component
public class CatalogValidator {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;
    private static final int CARDS_PER_FACTION = 16;
    private static final int MIN_STAT = 0;
    private static final int MAX_STAT = 1000;
    private static final Map<String, Integer> CARDS_PER_RARITY = rarityDistribution();

    public void validate(List<CatalogFile> files) {
        List<String> problems = new ArrayList<>();
        Set<String> factions = new HashSet<>();
        Set<String> codes = new HashSet<>();
        for (CatalogFile file : files) {
            if (!factions.add(file.faction())) {
                problems.add(file.faction() + " appears in more than one file");
            }
            checkFile(file, problems);
            for (CardEntry card : file.cards()) {
                if (!codes.add(card.id())) {
                    problems.add("Code " + card.id() + " is used by more than one card");
                }
                checkStats(card, problems);
            }
        }
        if (!problems.isEmpty()) {
            throw new CatalogValidationException(problems);
        }
    }

    private static void checkFile(CatalogFile file, List<String> problems) {
        if (file.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            problems.add(file.faction() + " uses schemaVersion " + file.schemaVersion()
                    + ", expected " + SUPPORTED_SCHEMA_VERSION);
        }
        if (file.cards().size() != CARDS_PER_FACTION) {
            problems.add(file.faction() + " has " + file.cards().size() + " cards, expected " + CARDS_PER_FACTION);
        }
        CARDS_PER_RARITY.forEach((rarity, expected) -> {
            long actual = file.cards().stream().filter(card -> rarity.equals(card.rarity())).count();
            if (actual != expected) {
                problems.add(file.faction() + " has " + actual + " " + rarity + " cards, expected " + expected);
            }
        });
    }

    private static void checkStats(CardEntry card, List<String> problems) {
        String label = "Card " + card.id() + " (" + card.name() + ")";
        CardStats stats = card.stats();
        if (stats == null) {
            problems.add(label + " has no stats");
            return;
        }
        checkStat(label, "atk", stats.atk(), problems);
        checkStat(label, "def", stats.def(), problems);
        checkStat(label, "vel", stats.vel(), problems);
        checkStat(label, "crt", stats.crt(), problems);
    }

    private static void checkStat(String label, String stat, int value, List<String> problems) {
        if (value < MIN_STAT || value > MAX_STAT) {
            problems.add(label + " has " + stat + " " + value + ", expected " + MIN_STAT + "-" + MAX_STAT);
        }
    }

    private static Map<String, Integer> rarityDistribution() {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("Comune", 8);
        distribution.put("Rara", 4);
        distribution.put("Epica", 3);
        distribution.put("Leggendaria", 1);
        return distribution;
    }
}
