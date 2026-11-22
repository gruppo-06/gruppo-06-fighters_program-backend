package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AdvancedMatchTest {

    private static final long SEED = 42L;
    private static final BaseRules RULES = new BaseRules(1, 1000, 2, 5, 10);

    private final MatchEngine engine = new MatchEngine();

    @Test
    void sameDecksAndSeedGiveTheSameAdvancedMatch() {
        List<FighterCard> deckOne = deck("A");
        List<FighterCard> deckTwo = deck("B");

        MatchResult first = engine.play(deckOne, deckTwo, SEED, new AdvancedRuleSet(RULES, true));
        MatchResult second = engine.play(deckOne, deckTwo, SEED, new AdvancedRuleSet(RULES, true));

        assertEquals(first, second);
    }

    @Test
    void differentSeedsCanProduceDifferentAdvancedMatches() {
        List<FighterCard> deckOne = deck("A");
        List<FighterCard> deckTwo = deck("B");

        boolean foundDifferentResult = false;
        for (long seed = 0; seed < 100 && !foundDifferentResult; seed++) {
            MatchResult first = engine.play(deckOne, deckTwo, seed, new AdvancedRuleSet(RULES, true));
            MatchResult second = engine.play(deckOne, deckTwo, seed + 1, new AdvancedRuleSet(RULES, true));
            foundDifferentResult = !first.equals(second);
        }

        assertTrue(foundDifferentResult);
    }

    @Test
    void advancedModeIsRecordedInTheMatchLog() {
        MatchResult result = engine.play(deck("A"), deck("B"), SEED, new AdvancedRuleSet(RULES, true));

        assertEquals(GameMode.ADVANCED, result.log().mode());
    }

    @Test
    void advancedMatchesEventuallyContainDodgesAndCriticalHits() {
        boolean foundDodge = false;
        boolean foundCritical = false;
        for (long seed = 0; seed < 100 && !(foundDodge && foundCritical); seed++) {
            MatchResult result = engine.play(deck("A"), deck("B"), seed, new AdvancedRuleSet(RULES, true));
            for (TurnLog turn : result.log().turns()) {
                foundDodge |= turn.fighterOne().dodged() || turn.fighterTwo().dodged();
                foundCritical |= turn.fighterOne().criticalHit() || turn.fighterTwo().criticalHit();
                assertTrue(turn.narration().contains("è più veloce e colpisce per primo."));
                if (turn.fighterOne().dodged() || turn.fighterTwo().dodged()) {
                    assertTrue(turn.narration().contains(" schiva il colpo di "));
                }
                if (turn.fighterOne().criticalHit() || turn.fighterTwo().criticalHit()) {
                    assertTrue(turn.narration().contains("Colpo critico di "));
                }
                if (!turn.fighterOne().struckFirst() && turn.fighterOne().defenseAfter() == 0) {
                    assertFalse(turn.narration().contains(turn.fighterOne().cardName() + " infligge"));
                }
                if (!turn.fighterTwo().struckFirst() && turn.fighterTwo().defenseAfter() == 0) {
                    assertFalse(turn.narration().contains(turn.fighterTwo().cardName() + " infligge"));
                }
            }
        }

        assertTrue(foundDodge);
        assertTrue(foundCritical);
    }

    @Test
    void baseModeRemainsUnchangedWhenTheSameDecksAndSeedArePlayed() {
        List<FighterCard> deckOne = deck("A");
        List<FighterCard> deckTwo = deck("B");

        MatchResult viaRules = engine.play(deckOne, deckTwo, SEED, RULES);
        MatchResult viaRuleSet = engine.play(deckOne, deckTwo, SEED, new BaseRuleSet(RULES));

        assertEquals(viaRules, viaRuleSet);
    }

    private static List<FighterCard> deck(String prefix) {
        return List.of(
                new FighterCard(prefix + "01", "Fighter " + prefix + "01", Faction.AFRICAN, 120, 1000, 850, 1000),
                new FighterCard(prefix + "02", "Fighter " + prefix + "02", Faction.AFRICAN, 120, 1000, 250, 0));
    }
}
