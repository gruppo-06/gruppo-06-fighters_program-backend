package it.fightersprogram.engine;

import java.util.List;

/**
 * Everything needed to replay a match without running the engine again. Only plain values, so it serialises to JSON.
 *
 * @param deckOneOrder card codes of player one in the exact order after shuffling
 * @param deckTwoOrder card codes of player two in the exact order after shuffling
 */
public record MatchLog(
        long seed,
        GameMode mode,
        int rulesVersion,
        List<String> deckOneOrder,
        List<String> deckTwoOrder,
        List<TurnLog> turns,
        MatchSummary summary) {

    public MatchLog {
        deckOneOrder = List.copyOf(deckOneOrder);
        deckTwoOrder = List.copyOf(deckTwoOrder);
        turns = List.copyOf(turns);
    }
}
