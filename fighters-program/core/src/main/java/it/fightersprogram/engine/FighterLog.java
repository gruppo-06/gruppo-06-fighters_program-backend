package it.fightersprogram.engine;

/**
 * One card's side of a round.
 *
 * @param deckPosition zero-based position in the shuffled deck; tells apart copies of the same card
 */
public record FighterLog(
        int deckPosition,
        String cardCode,
        String cardName,
        Faction faction,
        boolean hasFactionAdvantage,
        int printedAttack,
        int effectiveAttack,
        int damageDealt,
        int defenseBefore,
        int defenseAfter) {
}
