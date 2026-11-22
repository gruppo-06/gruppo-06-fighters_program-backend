package it.fightersprogram.engine;

import java.util.SplittableRandom;

/**
 * The advanced game mode: speed decides who strikes first, and every strike can be dodged, vary in damage or be
 * critical.
 */
public final class AdvancedRuleSet implements RuleSet {

    private static final long INITIATIVE_STREAM = 0x9E3779B97F4A7C15L;
    private static final long DODGE_STREAM = 0xC2B2AE3D27D4EB4FL;
    private static final long VARIANCE_STREAM = 0x165667B19E3779F9L;
    private static final long CRITICAL_STREAM = 0x27D4EB2F165667C5L;

    private static final int PERCENT = 100;
    private static final double MINIMUM_DODGE_CHANCE = 5.0;
    private static final double MAXIMUM_DODGE_CHANCE = 25.0;
    private static final double MINIMUM_CRITICAL_CHANCE = 5.0;
    private static final double MAXIMUM_CRITICAL_CHANCE = 40.0;

    private final BaseRules rules;
    private final boolean factionBonus;

    public AdvancedRuleSet(BaseRules rules, boolean factionBonus) {
        this.rules = rules;
        this.factionBonus = factionBonus;
    }

    @Override
    public GameMode mode() {
        return GameMode.ADVANCED;
    }

    @Override
    public BaseRules rules() {
        return rules;
    }

    @Override
    public Duel startMatch(long seed) {
        return new AdvancedDuel(rules.factionBonusPercent(), factionBonus,
                new SplittableRandom(seed ^ INITIATIVE_STREAM),
                new SplittableRandom(seed ^ DODGE_STREAM),
                new SplittableRandom(seed ^ VARIANCE_STREAM),
                new SplittableRandom(seed ^ CRITICAL_STREAM));
    }

    public static double dodgeChance(int defenderSpeed, int attackerSpeed) {
        double chance = MINIMUM_DODGE_CHANCE + ((double) defenderSpeed - attackerSpeed) / 50.0;
        return clamp(chance, 0.0, MAXIMUM_DODGE_CHANCE);
    }

    public static double criticalChance(int critical) {
        double chance = MINIMUM_CRITICAL_CHANCE + 0.035 * critical;
        return clamp(chance, MINIMUM_CRITICAL_CHANCE, MAXIMUM_CRITICAL_CHANCE);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class AdvancedDuel implements RuleSet.Duel {

        private final int factionBonusPercent;
        private final boolean factionBonus;
        private final SplittableRandom initiativeRandom;
        private final SplittableRandom dodgeRandom;
        private final SplittableRandom varianceRandom;
        private final SplittableRandom criticalRandom;

        private FighterCard pairingCardOne;
        private FighterCard pairingCardTwo;
        private boolean initiativeCoinDrawn;
        private boolean firstCardInitiates;
        private int lastKnockoutRawDamage;
        private FighterCard lastKnockoutLoser;

        AdvancedDuel(int factionBonusPercent, boolean factionBonus, SplittableRandom initiativeRandom,
                SplittableRandom dodgeRandom, SplittableRandom varianceRandom, SplittableRandom criticalRandom) {
            this.factionBonusPercent = factionBonusPercent;
            this.factionBonus = factionBonus;
            this.initiativeRandom = initiativeRandom;
            this.dodgeRandom = dodgeRandom;
            this.varianceRandom = varianceRandom;
            this.criticalRandom = criticalRandom;
        }

        @Override
        public RoundResult resolve(FighterInPlay one, FighterInPlay two) {
            lastKnockoutRawDamage = 0;
            lastKnockoutLoser = null;

            boolean oneInitiates = oneInitiates(one.card(), two.card());
            FighterInPlay initialAttacker = oneInitiates ? one : two;
            FighterInPlay initialDefender = oneInitiates ? two : one;
            StrikeResult initialStrike = strike(initialAttacker, initialDefender);

            StrikeResult strikeByOne = oneInitiates ? initialStrike : null;
            StrikeResult strikeByTwo = oneInitiates ? null : initialStrike;
            if (!initialStrike.knocksOut(initialDefender)) {
                StrikeResult secondStrike = strike(initialDefender, initialAttacker);
                strikeByOne = oneInitiates ? initialStrike : secondStrike;
                strikeByTwo = oneInitiates ? secondStrike : initialStrike;
                if (secondStrike.knocksOut(initialAttacker)) {
                    rememberKnockout(initialAttacker.card(), secondStrike.rawDamage());
                }
            } else {
                rememberKnockout(initialDefender.card(), initialStrike.rawDamage());
            }

            FighterRound fighterOne = round(one, two, strikeByOne, strikeByTwo, oneInitiates);
            FighterRound fighterTwo = round(two, one, strikeByTwo, strikeByOne, !oneInitiates);
            return new RoundResult(fighterOne, fighterTwo);
        }

        @Override
        public int healthDamage(FighterRound survivor, FighterRound loser) {
            if (lastKnockoutLoser == loser.card()) {
                return Math.max(0, lastKnockoutRawDamage - loser.defenseBefore());
            }
            return Math.max(0, survivor.effectiveAttack() - loser.defenseBefore());
        }

        private boolean oneInitiates(FighterCard cardOne, FighterCard cardTwo) {
            if (pairingCardOne != cardOne || pairingCardTwo != cardTwo) {
                pairingCardOne = cardOne;
                pairingCardTwo = cardTwo;
                initiativeCoinDrawn = false;
            }
            if (cardOne.speed() > cardTwo.speed()) {
                return true;
            }
            if (cardTwo.speed() > cardOne.speed()) {
                return false;
            }
            if (!initiativeCoinDrawn) {
                firstCardInitiates = initiativeRandom.nextBoolean();
                initiativeCoinDrawn = true;
            }
            return firstCardInitiates;
        }

        private StrikeResult strike(FighterInPlay attacker, FighterInPlay defender) {
            int effectiveAttack = effectiveAttack(attacker, defender);
            double dodgeChance = dodgeChance(defender.card().speed(), attacker.card().speed());
            boolean dodged = dodgeRandom.nextDouble() * PERCENT < dodgeChance;
            int rawDamage = 0;
            boolean criticalHit = false;
            if (!dodged) {
                double variance = 0.85 + 0.30 * varianceRandom.nextDouble();
                rawDamage = (int) Math.round(effectiveAttack * variance);
                double criticalChance = criticalChance(attacker.card().critical());
                criticalHit = criticalRandom.nextDouble() * PERCENT < criticalChance;
                if (criticalHit) {
                    rawDamage = (int) Math.round(rawDamage * 1.5);
                }
            }
            int damageDealt = Math.min(rawDamage, defender.remainingDefense());
            return new StrikeResult(effectiveAttack, rawDamage, damageDealt, dodged, criticalHit);
        }

        private int effectiveAttack(FighterInPlay attacker, FighterInPlay defender) {
            int attack = attacker.card().attack();
            if (!factionBonus || !hasAdvantage(attacker, defender)) {
                return attack;
            }
            return (attack * (PERCENT + factionBonusPercent) + PERCENT / 2) / PERCENT;
        }

        private static boolean hasAdvantage(FighterInPlay attacker, FighterInPlay defender) {
            return attacker.card().faction().hasAdvantageOver(defender.card().faction());
        }

        private FighterRound round(FighterInPlay fighter, FighterInPlay opponent, StrikeResult ownStrike,
                StrikeResult incomingStrike, boolean struckFirst) {
            int effectiveAttack = ownStrike == null ? effectiveAttack(fighter, opponent) : ownStrike.effectiveAttack();
            int damageDealt = ownStrike == null ? 0 : ownStrike.damageDealt();
            int defenseAfter = fighter.remainingDefense()
                    - (incomingStrike == null ? 0 : incomingStrike.rawDamage());
            return new FighterRound(fighter.card(), hasAdvantage(fighter, opponent), effectiveAttack, damageDealt,
                    fighter.remainingDefense(), Math.max(0, defenseAfter), struckFirst,
                    incomingStrike != null && incomingStrike.dodged(),
                    ownStrike != null && ownStrike.criticalHit());
        }

        private void rememberKnockout(FighterCard loser, int rawDamage) {
            lastKnockoutLoser = loser;
            lastKnockoutRawDamage = rawDamage;
        }
    }

    private record StrikeResult(int effectiveAttack, int rawDamage, int damageDealt, boolean dodged,
            boolean criticalHit) {

        boolean knocksOut(FighterInPlay defender) {
            return rawDamage >= defender.remainingDefense();
        }
    }
}
