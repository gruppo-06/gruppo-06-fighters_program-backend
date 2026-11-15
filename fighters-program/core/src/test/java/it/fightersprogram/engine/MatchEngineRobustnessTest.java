package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class MatchEngineRobustnessTest {

    private static final int MATCHES = 1000;
    private static final int DECK_SIZE = 16;
    private static final int MAX_TURNS = 100;
    private static final int MAX_STAT = 1000;
    private static final BaseRules RULES = new BaseRules(1, 3000, DECK_SIZE, 5, MAX_TURNS);

    private final MatchEngine engine = new MatchEngine();

    @Test
    void everyMatchEndsWithinTheTurnLimitForRandomDecksAndSeeds() {
        Random generator = new Random(2026L);

        for (int match = 0; match < MATCHES; match++) {
            List<FighterCard> deckOne = randomDeck(generator, "P1-");
            List<FighterCard> deckTwo = randomDeck(generator, "P2-");
            long seed = generator.nextLong();

            MatchResult result = assertDoesNotThrow(() -> engine.play(deckOne, deckTwo, seed, RULES));

            assertTrue(result.turnsPlayed() >= 1 && result.turnsPlayed() <= MAX_TURNS);
            assertTrue(result.healthOne() >= 0 && result.healthTwo() >= 0);
        }
    }

    private static List<FighterCard> randomDeck(Random generator, String codePrefix) {
        Faction[] factions = Faction.values();
        return IntStream.range(0, DECK_SIZE)
                .mapToObj(index -> new FighterCard(codePrefix + index, "Fighter " + codePrefix + index,
                        factions[generator.nextInt(factions.length)],
                        1 + generator.nextInt(MAX_STAT),
                        generator.nextInt(MAX_STAT + 1)))
                .toList();
    }
}
