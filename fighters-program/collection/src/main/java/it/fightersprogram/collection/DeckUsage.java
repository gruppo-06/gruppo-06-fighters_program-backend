package it.fightersprogram.collection;

/**
 * Implemented by the matches module, so a deck waiting in a challenge cannot be deleted without a dependency cycle.
 */
public interface DeckUsage {

    boolean isUsedInPendingChallenge(long deckId);
}
