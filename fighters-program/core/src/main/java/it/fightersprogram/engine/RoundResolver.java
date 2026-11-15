package it.fightersprogram.engine;

public final class RoundResolver {

    private static final int PERCENT = 100;

    private final int factionBonusPercent;

    public RoundResolver(int factionBonusPercent) {
        if (factionBonusPercent < 0) {
            throw new IllegalArgumentException("factionBonusPercent must not be negative, got " + factionBonusPercent);
        }
        this.factionBonusPercent = factionBonusPercent;
    }

    public RoundResult resolve(FighterInPlay first, FighterInPlay second) {
        return new RoundResult(strike(first, second), strike(second, first));
    }

    private FighterRound strike(FighterInPlay fighter, FighterInPlay opponent) {
        int effectiveAttack = effectiveAttack(fighter, opponent);
        int damageDealt = Math.min(effectiveAttack, opponent.remainingDefense());
        int defenseAfter = Math.max(0, fighter.remainingDefense() - effectiveAttack(opponent, fighter));
        return new FighterRound(fighter.card(), hasAdvantage(fighter, opponent), effectiveAttack, damageDealt,
                fighter.remainingDefense(), defenseAfter);
    }

    private int effectiveAttack(FighterInPlay attacker, FighterInPlay defender) {
        int attack = attacker.card().attack();
        return hasAdvantage(attacker, defender) ? withFactionBonus(attack) : attack;
    }

    private static boolean hasAdvantage(FighterInPlay attacker, FighterInPlay defender) {
        return attacker.card().faction().hasAdvantageOver(defender.card().faction());
    }

    // Provisional convention until SWD_EN01 (#24): the bonus is rounded half up on every hit (211 * 1.05 -> 222).
    private int withFactionBonus(int attack) {
        return (attack * (PERCENT + factionBonusPercent) + PERCENT / 2) / PERCENT;
    }
}
