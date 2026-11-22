package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AdvancedRuleSetTest {

    private static final BaseRules RULES = new BaseRules(1, 1000, 1, 5, 10);

    @Test
    void calculatesDodgeChanceWithinTheAllowedRange() {
        assertEquals(17.0, AdvancedRuleSet.dodgeChance(850, 250));
        assertEquals(0.0, AdvancedRuleSet.dodgeChance(250, 850));
        assertEquals(25.0, AdvancedRuleSet.dodgeChance(1000, 0));
        assertEquals(0.0, AdvancedRuleSet.dodgeChance(0, 1000));
    }

    @Test
    void calculatesCriticalChanceWithinTheAllowedRange() {
        assertEquals(19.0, AdvancedRuleSet.criticalChance(400));
        assertEquals(5.0, AdvancedRuleSet.criticalChance(0));
        assertEquals(40.0, AdvancedRuleSet.criticalChance(1000));
    }

    @Test
    void fasterFighterStrikesFirst() {
        FighterCard faster = card("AA01", 100, 1000, 850, 0);
        FighterCard slower = card("AA02", 100, 1000, 250, 0);
        RuleSet.Duel duel = new AdvancedRuleSet(RULES, false).startMatch(42L);

        RoundResult result = duel.resolve(FighterInPlay.enter(faster), FighterInPlay.enter(slower));

        assertTrue(result.first().struckFirst());
        assertFalse(result.second().struckFirst());
    }

    @Test
    void reportsFactionAdvantageEvenWhenTheFactionBonusIsDisabled() {
        FighterCard attacker = new FighterCard("IP01", "Attacker", Faction.INDO_PACIFIC, 211, 1000, 850, 0);
        FighterCard defender = new FighterCard("EA01", "Defender", Faction.EURO_ATLANTIC, 200, 1000, 250, 0);

        RoundResult withoutBonus = new AdvancedRuleSet(RULES, false).startMatch(42L)
                .resolve(FighterInPlay.enter(attacker), FighterInPlay.enter(defender));
        RoundResult withBonus = new AdvancedRuleSet(RULES, true).startMatch(42L)
                .resolve(FighterInPlay.enter(attacker), FighterInPlay.enter(defender));

        assertTrue(withoutBonus.first().hasFactionAdvantage());
        assertEquals(211, withoutBonus.first().effectiveAttack());
        assertEquals(222, withBonus.first().effectiveAttack());
    }

    @Test
    void keepsTheEqualSpeedInitiativeWhileTheSameCardsFaceEachOther() {
        FighterCard one = card("AA01", 100, 1000, 500, 0);
        FighterCard two = card("AA02", 100, 1000, 500, 0);
        RuleSet.Duel duel = new AdvancedRuleSet(RULES, false).startMatch(42L);

        RoundResult first = duel.resolve(FighterInPlay.enter(one), FighterInPlay.enter(two));
        RoundResult second = duel.resolve(FighterInPlay.enter(one), FighterInPlay.enter(two));

        assertEquals(first.first().struckFirst(), second.first().struckFirst());
        assertEquals(first.second().struckFirst(), second.second().struckFirst());
    }

    @Test
    void aKnockoutByTheFirstStrikerPreventsTheSecondFighterFromStrikingBack() {
        FighterCard faster = card("AA01", 1000, 1000, 1000, 0);
        FighterCard slower = card("AA02", 100, 100, 0, 0);
        RuleSet.Duel duel = new AdvancedRuleSet(RULES, false).startMatch(42L);

        RoundResult result = duel.resolve(FighterInPlay.enter(faster), FighterInPlay.enter(slower));

        assertEquals(RoundOutcome.SECOND_KNOCKED_OUT, result.outcome());
        assertEquals(100, result.first().damageDealt());
        assertEquals(0, result.second().damageDealt());
        assertFalse(result.second().struckFirst());
    }

    @Test
    void knockoutHealthDamageUsesTheRawDamageBeforeCapping() {
        FighterCard faster = card("AA01", 1000, 1000, 500, 0);
        FighterCard slower = card("AA02", 100, 100, 0, 0);
        RuleSet.Duel duel = new AdvancedRuleSet(RULES, false).startMatch(42L);

        RoundResult result = duel.resolve(FighterInPlay.enter(faster), FighterInPlay.enter(slower));
        int healthDamage = duel.healthDamage(result.first(), result.second());

        assertEquals(100, result.first().damageDealt());
        assertTrue(healthDamage > 0);
    }

    @Test
    void damageStaysWithinTheAdvancedRange() {
        FighterCard attacker = card("AA01", 100, 1000, 500, 1000);
        FighterCard defender = card("AA02", 100, 1000, 500, 0);
        RuleSet.Duel duel = new AdvancedRuleSet(RULES, false).startMatch(42L);

        RoundResult result = duel.resolve(FighterInPlay.enter(attacker), FighterInPlay.enter(defender));

        if (result.first().damageDealt() > 0) {
            assertTrue(result.first().damageDealt() >= Math.round(0.85 * 100));
            assertTrue(result.first().damageDealt() <= Math.round(1.15 * 100 * 1.5));
        }
        if (result.second().damageDealt() > 0) {
            assertTrue(result.second().damageDealt() >= Math.round(0.85 * 100));
            assertTrue(result.second().damageDealt() <= Math.round(1.15 * 100 * 1.5));
        }
    }

    private static FighterCard card(String code, int attack, int defense, int speed, int critical) {
        return new FighterCard(code, "Fighter " + code, Faction.AFRICAN, attack, defense, speed, critical);
    }
}
