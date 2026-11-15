package it.fightersprogram.engine;

/**
 * Values of the base game mode. The final numbers are decided in SWD_EN01 (#11) and passed in by the caller,
 * so the engine never hardcodes them.
 *
 * @param factionBonusPercent extra attack, in percent, of a card whose faction has the advantage
 * @param maxTurns            number of turns after which the match ends; a turn lasts until at least one card is KO
 */
public record BaseRules(int rulesVersion, int startingHealth, int deckSize, int factionBonusPercent, int maxTurns) {

    public BaseRules {
        requirePositive(rulesVersion, "rulesVersion");
        requirePositive(startingHealth, "startingHealth");
        requirePositive(deckSize, "deckSize");
        requirePositive(maxTurns, "maxTurns");
        if (factionBonusPercent < 0) {
            throw new IllegalArgumentException("factionBonusPercent must not be negative, got " + factionBonusPercent);
        }
    }

    private static void requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive, got " + value);
        }
    }
}
