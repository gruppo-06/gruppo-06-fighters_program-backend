package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The two playtest cases of the DSG balance document ("Bilanciamento di gioco"), with the delivered card stats.
 */
class BalanceCasesTest {

    private static final long SEED = 7L;
    private static final int STARTING_HEALTH = 1000;
    private static final int FACTION_BONUS_PERCENT = 5;
    private static final BaseRules ONE_CARD_DUEL = new BaseRules(1, STARTING_HEALTH, 1, FACTION_BONUS_PERCENT, 1);

    private static final FighterCard HANA_KURODA =
            new FighterCard("EA16", "Hana Kuroda - Capitana", Faction.EURO_ATLANTIC, 281, 540);
    private static final FighterCard DARIUS_BELL =
            new FighterCard("EA10", "Darius Bell", Faction.EURO_ATLANTIC, 318, 680);
    private static final FighterCard ARJUN_RAO =
            new FighterCard("IP16", "Arjun Rao - Capitano", Faction.INDO_PACIFIC, 211, 896);
    private static final FighterCard NOAH_BISSET =
            new FighterCard("EA02", "Noah Bisset", Faction.EURO_ATLANTIC, 296, 666);

    private final MatchEngine engine = new MatchEngine();

    @Test
    void theRareDariusBellBeatsTheLegendaryHanaKurodaInTheSecondRoundWithoutBonus() {
        MatchResult result = engine.play(List.of(HANA_KURODA), List.of(DARIUS_BELL), SEED, ONE_CARD_DUEL);
        TurnLog lastRound = result.log().turns().getLast();

        assertEquals(2, lastRound.roundNumber());
        assertEquals(RoundOutcome.FIRST_KNOCKED_OUT, lastRound.outcome());
        assertFalse(lastRound.fighterTwo().hasFactionAdvantage());
        assertEquals(118, lastRound.fighterTwo().defenseAfter());
        assertEquals(318 - 222, lastRound.healthDamage());
        assertEquals(MatchOutcome.PLAYER_TWO_WINS, result.outcome());
    }

    // The DSG analysis keeps the decimals (221.55 per hit), which leaves Noah at 1.35 DEF after three hits and ends
    // in a double KO in round 4. The engine rounds the bonus on every hit (222), so Arjun wins in round 3: this is one
    // of the four pairs of the catalog whose outcome depends on the rounding convention still open in #24.
    @Test
    void arjunRaoBeatsNoahBissetInTheThirdRoundBecauseTheBonusIsRoundedOnEveryHit() {
        MatchResult result = engine.play(List.of(ARJUN_RAO), List.of(NOAH_BISSET), SEED, ONE_CARD_DUEL);
        TurnLog lastRound = result.log().turns().getLast();

        assertEquals(222, lastRound.fighterOne().effectiveAttack());
        assertEquals(3, lastRound.roundNumber());
        assertEquals(RoundOutcome.SECOND_KNOCKED_OUT, lastRound.outcome());
        assertEquals(896 - 3 * 296, lastRound.fighterOne().defenseAfter());
        assertEquals(MatchOutcome.PLAYER_ONE_WINS, result.outcome());
    }
}
