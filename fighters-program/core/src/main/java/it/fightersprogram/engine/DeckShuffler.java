package it.fightersprogram.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Shuffles decks with a caller-provided seed. Decks shuffled by the same instance draw from one random sequence,
 * so the order in which they are shuffled is part of the result.
 */
public final class DeckShuffler {

    private final Random random;

    public DeckShuffler(long seed) {
        this.random = new Random(seed);
    }

    public List<FighterCard> shuffle(List<FighterCard> deck) {
        List<FighterCard> shuffled = new ArrayList<>(deck);
        Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled);
    }
}
