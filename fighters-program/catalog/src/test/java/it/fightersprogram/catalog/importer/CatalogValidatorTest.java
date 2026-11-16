package it.fightersprogram.catalog.importer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

class CatalogValidatorTest {

    private final CatalogValidator validator = new CatalogValidator();

    @Test
    void acceptsCompleteFactionFiles() {
        assertDoesNotThrow(() -> validator.validate(List.of(validFile("Euroatlantica", "EA"),
                validFile("Africana", "AF"))));
    }

    @Test
    void rejectsAFactionWithoutSixteenCards() {
        List<CardEntry> cards = new ArrayList<>(validFile("Africana", "AF").cards());
        cards.removeFirst();

        assertProblem(new CatalogFile(1, "Africana", cards), "Africana has 15 cards, expected 16");
    }

    @Test
    void rejectsAWrongRarityDistribution() {
        CatalogFile file = withCard(validFile("Africana", "AF"), 0, entry -> copy(entry, "Rara", entry.stats()));

        assertProblem(file, "Africana has 7 Comune cards, expected 8");
    }

    @Test
    void rejectsDuplicateCodes() {
        CatalogFile file = withCard(validFile("Africana", "AF"), 1,
                entry -> new CardEntry("AF01", entry.name(), entry.country(), entry.fighterClass(), entry.rarity(),
                        entry.stats(), entry.copy(), entry.image()));

        assertProblem(file, "Code AF01 is used by more than one card");
    }

    @Test
    void rejectsAStatOutOfRange() {
        CatalogFile file = withCard(validFile("Africana", "AF"), 2,
                entry -> copy(entry, entry.rarity(), new CardStats(1200, 500, 500, 500)));

        assertProblem(file, "Card AF03 (Fighter AF03) has atk 1200, expected 0-1000");
    }

    @Test
    void rejectsACardWithoutStats() {
        CatalogFile file = withCard(validFile("Africana", "AF"), 2, entry -> copy(entry, entry.rarity(), null));

        assertProblem(file, "Card AF03 (Fighter AF03) has no stats");
    }

    @Test
    void rejectsAnUnsupportedSchemaVersion() {
        CatalogFile valid = validFile("Africana", "AF");

        assertProblem(new CatalogFile(2, "Africana", valid.cards()), "Africana uses schemaVersion 2, expected 1");
    }

    @Test
    void rejectsTwoFilesForTheSameFaction() {
        CatalogValidationException exception = assertThrows(CatalogValidationException.class,
                () -> validator.validate(List.of(validFile("Africana", "AF"), validFile("Africana", "AX"))));

        assertTrue(exception.getMessage().contains("Africana appears in more than one file"), exception.getMessage());
    }

    @Test
    void reportsEveryProblemAtOnce() {
        CatalogFile file = withCard(validFile("Africana", "AF"), 2,
                entry -> copy(entry, "Rara", new CardStats(-1, 500, 500, 500)));

        CatalogValidationException exception =
                assertThrows(CatalogValidationException.class, () -> validator.validate(List.of(file)));

        assertTrue(exception.getMessage().contains("has atk -1"), exception.getMessage());
        assertTrue(exception.getMessage().contains("7 Comune cards"), exception.getMessage());
        assertTrue(exception.getMessage().contains("5 Rara cards"), exception.getMessage());
    }

    private void assertProblem(CatalogFile file, String expectedProblem) {
        CatalogValidationException exception =
                assertThrows(CatalogValidationException.class, () -> validator.validate(List.of(file)));

        assertTrue(exception.getMessage().contains(expectedProblem), exception.getMessage());
    }

    private static CatalogFile validFile(String faction, String codePrefix) {
        List<String> rarities = new ArrayList<>();
        rarities.addAll(List.of("Comune", "Comune", "Comune", "Comune", "Comune", "Comune", "Comune", "Comune"));
        rarities.addAll(List.of("Rara", "Rara", "Rara", "Rara", "Epica", "Epica", "Epica", "Leggendaria"));
        List<CardEntry> cards = new ArrayList<>();
        for (int index = 0; index < rarities.size(); index++) {
            String code = "%s%02d".formatted(codePrefix, index + 1);
            cards.add(new CardEntry(code, "Fighter " + code, "Paese", "Combattenti", rarities.get(index),
                    new CardStats(300, 500, 400, 200), "Flavor",
                    new CardImage(code + ".png", "image/png", 900, 1200)));
        }
        return new CatalogFile(1, faction, cards);
    }

    private static CatalogFile withCard(CatalogFile file, int index, UnaryOperator<CardEntry> change) {
        List<CardEntry> cards = new ArrayList<>(file.cards());
        cards.set(index, change.apply(cards.get(index)));
        return new CatalogFile(file.schemaVersion(), file.faction(), cards);
    }

    private static CardEntry copy(CardEntry entry, String rarity, CardStats stats) {
        return new CardEntry(entry.id(), entry.name(), entry.country(), entry.fighterClass(), rarity, stats,
                entry.copy(), entry.image());
    }
}
