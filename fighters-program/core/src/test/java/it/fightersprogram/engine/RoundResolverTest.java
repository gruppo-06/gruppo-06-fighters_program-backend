package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RoundResolverTest {

    private static final int FACTION_BONUS_PERCENT = 5;

    private final RoundResolver resolver = new RoundResolver(FACTION_BONUS_PERCENT);

    @Test
    void bothCardsSurviveWhenNeitherAttackBreaksTheDefense() {
        FighterInPlay hana = FighterInPlay.enter(card("EA16", Faction.EURO_ATLANTIC, 281, 540));
        FighterInPlay darius = FighterInPlay.enter(card("EA10", Faction.EURO_ATLANTIC, 300, 680));

        RoundResult result = resolver.resolve(hana, darius);

        assertEquals(RoundOutcome.NO_KNOCKOUT, result.outcome());
        assertEquals(240, result.first().defenseAfter());
        assertEquals(399, result.second().defenseAfter());
        assertEquals(281, result.first().damageDealt());
        assertEquals(300, result.second().damageDealt());
    }

    @Test
    void onlyTheCardWhoseDefenseDropsToZeroIsKnockedOut() {
        FighterInPlay strong = FighterInPlay.enter(card("AA01", Faction.AFRICAN, 500, 600));
        FighterInPlay weak = FighterInPlay.enter(card("AA02", Faction.AFRICAN, 100, 400));

        RoundResult result = resolver.resolve(strong, weak);

        assertEquals(RoundOutcome.SECOND_KNOCKED_OUT, result.outcome());
        assertFalse(result.first().isKnockedOut());
        assertTrue(result.second().isKnockedOut());
        assertEquals(0, result.second().defenseAfter());
        assertEquals(400, result.first().damageDealt());
    }

    @Test
    void bothCardsAreKnockedOutWhenBothDefensesDropToZero() {
        FighterInPlay first = FighterInPlay.enter(card("AA01", Faction.AFRICAN, 500, 300));
        FighterInPlay second = FighterInPlay.enter(card("AA02", Faction.AFRICAN, 400, 450));

        RoundResult result = resolver.resolve(first, second);

        assertEquals(RoundOutcome.DOUBLE_KNOCKOUT, result.outcome());
    }

    @Test
    void damageIsComputedOnTheDefensesAtTheStartOfTheRound() {
        FighterInPlay first = new FighterInPlay(card("AA01", Faction.AFRICAN, 200, 900), 150);
        FighterInPlay second = new FighterInPlay(card("AA02", Faction.AFRICAN, 150, 900), 120);

        RoundResult result = resolver.resolve(first, second);

        assertEquals(150, result.first().defenseBefore());
        assertEquals(120, result.second().defenseBefore());
        assertEquals(RoundOutcome.DOUBLE_KNOCKOUT, result.outcome());
    }

    @Test
    void factionAdvantageRaisesAttackRoundedHalfUpOnEveryHit() {
        FighterInPlay arjun = FighterInPlay.enter(card("IP16", Faction.INDO_PACIFIC, 211, 900));
        FighterInPlay noah = FighterInPlay.enter(card("EA02", Faction.EURO_ATLANTIC, 200, 900));

        RoundResult result = resolver.resolve(arjun, noah);

        assertTrue(result.first().hasFactionAdvantage());
        assertFalse(result.second().hasFactionAdvantage());
        assertEquals(222, result.first().effectiveAttack());
        assertEquals(200, result.second().effectiveAttack());
        assertEquals(678, result.second().defenseAfter());
    }

    @Test
    void resultDoesNotDependOnWhichCardIsPassedFirst() {
        FighterInPlay arjun = new FighterInPlay(card("IP16", Faction.INDO_PACIFIC, 211, 900), 300);
        FighterInPlay noah = new FighterInPlay(card("EA02", Faction.EURO_ATLANTIC, 350, 900), 210);

        RoundResult forward = resolver.resolve(arjun, noah);
        RoundResult backward = resolver.resolve(noah, arjun);

        assertEquals(forward.first(), backward.second());
        assertEquals(forward.second(), backward.first());
    }

    private static FighterCard card(String code, Faction faction, int attack, int defense) {
        return new FighterCard(code, "Fighter " + code, faction, attack, defense);
    }
}
