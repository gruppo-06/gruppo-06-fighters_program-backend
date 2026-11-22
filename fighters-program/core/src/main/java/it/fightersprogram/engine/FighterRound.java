package it.fightersprogram.engine;

public record FighterRound(
        FighterCard card,
        boolean hasFactionAdvantage,
        int effectiveAttack,
        int damageDealt,
        int defenseBefore,
        int defenseAfter,
        boolean struckFirst,
        boolean dodged,
        boolean criticalHit) {

    public FighterRound(FighterCard card, boolean hasFactionAdvantage, int effectiveAttack, int damageDealt,
            int defenseBefore, int defenseAfter) {
        this(card, hasFactionAdvantage, effectiveAttack, damageDealt, defenseBefore, defenseAfter, false, false, false);
    }

    public boolean isKnockedOut() {
        return defenseAfter == 0;
    }
}
