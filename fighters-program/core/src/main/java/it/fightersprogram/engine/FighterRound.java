package it.fightersprogram.engine;

public record FighterRound(
        FighterCard card,
        boolean hasFactionAdvantage,
        int effectiveAttack,
        int damageDealt,
        int defenseBefore,
        int defenseAfter) {

    public boolean isKnockedOut() {
        return defenseAfter == 0;
    }
}
