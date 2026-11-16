package it.fightersprogram.engine;

import java.util.List;

/**
 * @param cards one entry per card that fought at least one round, player one first, in deck order
 */
public record MatchSummary(MatchOutcome outcome, MatchEndReason endReason, int totalTurns, List<CardSummary> cards) {

    public MatchSummary {
        cards = List.copyOf(cards);
    }
}
