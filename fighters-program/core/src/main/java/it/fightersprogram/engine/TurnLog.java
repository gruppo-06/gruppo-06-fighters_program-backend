package it.fightersprogram.engine;

/**
 * One round of a turn. Only the round that ends a turn can carry health damage and replacements.
 *
 * @param healthDamage   health lost by the player whose card was the only one knocked out; zero otherwise
 * @param replacementOne card drawn by player one after this round, or {@code null}
 * @param replacementTwo card drawn by player two after this round, or {@code null}
 */
public record TurnLog(
        int turnNumber,
        int roundNumber,
        FighterLog fighterOne,
        FighterLog fighterTwo,
        RoundOutcome outcome,
        int healthDamage,
        int healthOneAfter,
        int healthTwoAfter,
        CardRef replacementOne,
        CardRef replacementTwo,
        String narration) {

    TurnLog withNarration(String text) {
        return new TurnLog(turnNumber, roundNumber, fighterOne, fighterTwo, outcome, healthDamage, healthOneAfter,
                healthTwoAfter, replacementOne, replacementTwo, text);
    }
}
