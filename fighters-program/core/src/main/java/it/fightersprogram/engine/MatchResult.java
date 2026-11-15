package it.fightersprogram.engine;

public record MatchResult(
        MatchOutcome outcome,
        MatchEndReason endReason,
        int turnsPlayed,
        int healthOne,
        int healthTwo) {
}
