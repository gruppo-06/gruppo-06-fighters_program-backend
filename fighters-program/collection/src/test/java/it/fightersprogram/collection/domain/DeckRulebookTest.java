package it.fightersprogram.collection.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import it.fightersprogram.collection.DeckRules;
import it.fightersprogram.collection.Violation;

class DeckRulebookTest {

    private static final DeckRules RULES = new DeckRules(10, 2, 2, 1, 18, 10,
            Map.of("Comune", 1, "Rara", 2, "Epica", 3, "Leggendaria", 5));

    private final DeckRulebook rulebook = new DeckRulebook(RULES);

    @Test
    void acceptsADeckThatFollowsEveryRule() {
        assertTrue(rulebook.check(validDeck()).isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenDecks")
    void reportsEachBrokenRule(String expectedCode, List<DeckLine> deck) {
        List<String> codes = rulebook.check(deck).stream().map(Violation::code).toList();

        assertEquals(List.of(expectedCode), codes);
    }

    @Test
    void tooManyCopiesNamesTheCardInTheMessage() {
        List<DeckLine> deck = commons(7);
        deck.add(new DeckLine(99, "Hana Kuroda", "Comune", 3, 3));

        Violation violation = rulebook.check(deck).getFirst();

        assertEquals("Puoi usare al massimo 2 copie di Hana Kuroda", violation.message());
    }

    @Test
    void notOwnedSaysHowManyCopiesAreMissing() {
        List<DeckLine> deck = commons(8);
        deck.add(new DeckLine(99, "Darius Bell", "Rara", 2, 0));

        Violation violation = rulebook.check(deck).getFirst();

        assertEquals("NOT_OWNED", violation.code());
        assertEquals("Ti mancano 2 copie di Darius Bell", violation.message());
    }

    @Test
    void usesTheSingularForASingleMissingCopy() {
        List<DeckLine> deck = commons(9);
        deck.add(new DeckLine(99, "Darius Bell", "Rara", 1, 0));

        assertEquals("Ti manca 1 copia di Darius Bell", rulebook.check(deck).getFirst().message());
    }

    @Test
    void reportsEveryProblemAtOnce() {
        List<DeckLine> deck = List.of(new DeckLine(1, "Legend", "Leggendaria", 2, 0));

        List<String> codes = rulebook.check(deck).stream().map(Violation::code).toList();

        assertEquals(List.of("WRONG_SIZE", "TOO_MANY_LEGENDARY", "NOT_OWNED"), codes);
    }

    static Stream<Arguments> brokenDecks() {
        List<DeckLine> wrongSize = commons(4);

        List<DeckLine> tooManyCopies = commons(7);
        tooManyCopies.add(new DeckLine(90, "Triple", "Comune", 3, 3));

        List<DeckLine> tooManyEpic = commons(7);
        tooManyEpic.addAll(epics(3));

        List<DeckLine> tooManyLegendary = commons(8);
        tooManyLegendary.add(new DeckLine(80, "Legend A", "Leggendaria", 1, 1));
        tooManyLegendary.add(new DeckLine(81, "Legend B", "Leggendaria", 1, 1));

        List<DeckLine> overBudget = new ArrayList<>(List.of(
                new DeckLine(80, "Legend", "Leggendaria", 1, 1)));
        overBudget.addAll(epics(2));
        overBudget.add(new DeckLine(60, "Rare", "Rara", 2, 2));
        overBudget.add(new DeckLine(61, "Rare B", "Rara", 1, 1));
        overBudget.addAll(commons(4));

        List<DeckLine> notOwned = commons(8);
        notOwned.add(new DeckLine(70, "Borrowed", "Comune", 1, 0));
        notOwned.add(new DeckLine(71, "Owned", "Comune", 1, 1));

        return Stream.of(
                Arguments.of("WRONG_SIZE", wrongSize),
                Arguments.of("TOO_MANY_COPIES", tooManyCopies),
                Arguments.of("TOO_MANY_EPIC", tooManyEpic),
                Arguments.of("TOO_MANY_LEGENDARY", tooManyLegendary),
                Arguments.of("OVER_BUDGET", overBudget),
                Arguments.of("NOT_OWNED", notOwned));
    }

    private static List<DeckLine> validDeck() {
        List<DeckLine> deck = new ArrayList<>(List.of(new DeckLine(80, "Legend", "Leggendaria", 1, 1)));
        deck.add(new DeckLine(50, "Epic", "Epica", 1, 1));
        deck.add(new DeckLine(60, "Rare", "Rara", 2, 2));
        deck.addAll(commons(6));
        return deck;
    }

    private static List<DeckLine> commons(int count) {
        List<DeckLine> lines = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            lines.add(new DeckLine(index, "Common " + index, "Comune", 1, 2));
        }
        return lines;
    }

    private static List<DeckLine> epics(int count) {
        List<DeckLine> lines = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            lines.add(new DeckLine(50 + index, "Epic " + index, "Epica", 1, 1));
        }
        return lines;
    }
}
