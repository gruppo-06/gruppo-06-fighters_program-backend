package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class DeckShufflerTest {

    private static final List<FighterCard> DECK = IntStream.rangeClosed(1, 16)
            .mapToObj(number -> new FighterCard("EA%02d".formatted(number), "Fighter " + number,
                    Faction.EURO_ATLANTIC, 100, 100))
            .toList();

    @Test
    void sameSeedGivesTheSameOrder() {
        assertEquals(new DeckShuffler(42L).shuffle(DECK), new DeckShuffler(42L).shuffle(DECK));
    }

    @Test
    void differentSeedsGiveDifferentOrders() {
        assertNotEquals(new DeckShuffler(42L).shuffle(DECK), new DeckShuffler(43L).shuffle(DECK));
    }

    @Test
    void sameSeedShufflesTwoDecksInARepeatableSequence() {
        DeckShuffler firstRun = new DeckShuffler(7L);
        DeckShuffler secondRun = new DeckShuffler(7L);

        List<List<FighterCard>> first = List.of(firstRun.shuffle(DECK), firstRun.shuffle(DECK));
        List<List<FighterCard>> second = List.of(secondRun.shuffle(DECK), secondRun.shuffle(DECK));

        assertEquals(first, second);
    }

    @Test
    void keepsEveryCardAndLeavesTheOriginalDeckUntouched() {
        List<FighterCard> original = List.copyOf(DECK);

        List<FighterCard> shuffled = new DeckShuffler(42L).shuffle(DECK);

        assertEquals(original, DECK);
        assertEquals(DECK.size(), shuffled.size());
        assertEquals(Set.copyOf(DECK), Set.copyOf(shuffled));
    }
}
