package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class MatchEngineTest {

    private static final long SEED = 42L;
    private static final int STARTING_HEALTH = 1000;
    private static final int FACTION_BONUS_PERCENT = 5;

    private final MatchEngine engine = new MatchEngine();

    @Test
    void survivorDealsItsAttackMinusTheLoserDefenseToHealth() {
        MatchResult result = engine.play(List.of(striker()), List.of(weakling()), SEED, rules(1, 10));

        assertEquals(STARTING_HEALTH, result.healthOne());
        assertEquals(STARTING_HEALTH - 100, result.healthTwo());
    }

    @Test
    void playerWhoCannotReplaceAKnockedOutCardLosesForExhaustedDeck() {
        MatchResult result = engine.play(List.of(striker()), List.of(weakling()), SEED, rules(1, 10));

        assertEquals(MatchOutcome.PLAYER_ONE_WINS, result.outcome());
        assertEquals(MatchEndReason.DECK_EXHAUSTED, result.endReason());
        assertEquals(1, result.turnsPlayed());
    }

    @Test
    void matchEndsWhenHealthDropsToZero() {
        BaseRules lowHealth = new BaseRules(1, 50, 1, FACTION_BONUS_PERCENT, 10);

        MatchResult result = engine.play(List.of(striker()), List.of(weakling()), SEED, lowHealth);

        assertEquals(MatchOutcome.PLAYER_ONE_WINS, result.outcome());
        assertEquals(MatchEndReason.HEALTH_DEPLETED, result.endReason());
        assertEquals(0, result.healthTwo());
    }

    @Test
    void doubleKnockoutDealsNoHealthDamage() {
        FighterCard mirror = card("AA01", 500, 400);

        MatchResult result = engine.play(List.of(mirror), List.of(mirror), SEED, rules(1, 10));

        assertEquals(STARTING_HEALTH, result.healthOne());
        assertEquals(STARTING_HEALTH, result.healthTwo());
    }

    @Test
    void bothDecksExhaustedAtOnceIsDecidedByHealth() {
        FighterCard mirror = card("AA01", 500, 400);

        MatchResult result = engine.play(List.of(mirror), List.of(mirror), SEED, rules(1, 10));

        assertEquals(MatchOutcome.DRAW, result.outcome());
        assertEquals(MatchEndReason.DECK_EXHAUSTED, result.endReason());
    }

    @Test
    void survivorKeepsItsRemainingDefenseIntoTheNextTurn() {
        FighterCard veteran = card("AA01", 400, 500);
        FighterCard recruit = card("AA02", 300, 350);

        MatchResult result = engine.play(List.of(veteran, veteran), List.of(recruit, recruit), SEED, rules(2, 10));

        assertEquals(STARTING_HEALTH - 50, result.healthTwo());
        assertEquals(MatchOutcome.PLAYER_ONE_WINS, result.outcome());
        assertEquals(2, result.turnsPlayed());
    }

    @Test
    void turnLimitIsWonByTheHealthierPlayer() {
        MatchResult result = engine.play(List.of(striker(), striker()), List.of(weakling(), weakling()), SEED,
                rules(2, 1));

        assertEquals(MatchOutcome.PLAYER_ONE_WINS, result.outcome());
        assertEquals(MatchEndReason.TURN_LIMIT, result.endReason());
        assertEquals(1, result.turnsPlayed());
    }

    @Test
    void turnLimitWithEqualHealthIsADraw() {
        FighterCard mirror = card("AA01", 500, 400);

        MatchResult result = engine.play(List.of(mirror, mirror), List.of(mirror, mirror), SEED, rules(2, 1));

        assertEquals(MatchOutcome.DRAW, result.outcome());
        assertEquals(MatchEndReason.TURN_LIMIT, result.endReason());
    }

    @Test
    void factionBonusAlsoRaisesHealthDamage() {
        FighterCard attacker = new FighterCard("IP01", "Attacker", Faction.INDO_PACIFIC, 400, 900);
        FighterCard defender = new FighterCard("EA01", "Defender", Faction.EURO_ATLANTIC, 100, 300);

        MatchResult result = engine.play(List.of(attacker), List.of(defender), SEED, rules(1, 10));

        assertEquals(STARTING_HEALTH - (420 - 300), result.healthTwo());
    }

    @Test
    void sameDecksAndSeedGiveTheSameMatch() {
        List<FighterCard> deckOne = List.of(card("AA01", 300, 500), card("AA02", 450, 200), card("AA03", 120, 800));
        List<FighterCard> deckTwo = List.of(card("AA04", 250, 600), card("AA05", 500, 150), card("AA06", 350, 350));

        assertEquals(engine.play(deckOne, deckTwo, SEED, rules(3, 10)), engine.play(deckOne, deckTwo, SEED, rules(3, 10)));
    }

    @Test
    void rejectsDecksOfTheWrongSize() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.play(List.of(striker()), List.of(weakling(), weakling()), SEED, rules(1, 10)));
    }

    private static BaseRules rules(int deckSize, int maxTurns) {
        return new BaseRules(1, STARTING_HEALTH, deckSize, FACTION_BONUS_PERCENT, maxTurns);
    }

    private static FighterCard striker() {
        return card("AA10", 500, 600);
    }

    private static FighterCard weakling() {
        return card("AA20", 100, 400);
    }

    private static FighterCard card(String code, int attack, int defense) {
        return new FighterCard(code, "Fighter " + code, Faction.AFRICAN, attack, defense);
    }
}
