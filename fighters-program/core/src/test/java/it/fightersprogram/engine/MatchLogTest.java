package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class MatchLogTest {

    private static final long SEED = 42L;
    private static final int STARTING_HEALTH = 1000;

    private final MatchEngine engine = new MatchEngine();

    @Test
    void recordsSeedModeRulesVersionAndShuffledDeckOrders() {
        List<FighterCard> deckOne = randomDeck(new Random(1L), "A");
        List<FighterCard> deckTwo = randomDeck(new Random(2L), "B");
        BaseRules rules = new BaseRules(3, STARTING_HEALTH, deckOne.size(), 5, 100);
        DeckShuffler expectedShuffler = new DeckShuffler(SEED);

        MatchLog log = engine.play(deckOne, deckTwo, SEED, rules).log();

        assertEquals(SEED, log.seed());
        assertEquals(GameMode.BASE, log.mode());
        assertEquals(3, log.rulesVersion());
        assertEquals(codes(expectedShuffler.shuffle(deckOne)), log.deckOneOrder());
        assertEquals(codes(expectedShuffler.shuffle(deckTwo)), log.deckTwoOrder());
    }

    @Test
    void replayingTheLogFromTheStartGivesConsistentDefensesAndHealth() {
        for (long seed = 0; seed < 50; seed++) {
            List<FighterCard> deckOne = randomDeck(new Random(seed), "A");
            List<FighterCard> deckTwo = randomDeck(new Random(seed + 1000), "B");
            BaseRules rules = new BaseRules(1, STARTING_HEALTH, deckOne.size(), 5, 100);

            MatchResult result = engine.play(deckOne, deckTwo, seed, rules);

            assertLogIsConsistent(result);
        }
    }

    @Test
    void recordsEveryRoundWithReplacementsAndNarration() {
        MatchLog log = playVeteranAgainstRecruits().log();

        assertEquals(2, log.turns().size());
        TurnLog first = log.turns().get(0);
        assertEquals(1, first.turnNumber());
        assertEquals(1, first.roundNumber());
        assertEquals(RoundOutcome.SECOND_KNOCKED_OUT, first.outcome());
        assertEquals(350, first.fighterOne().damageDealt());
        assertEquals(200, first.fighterOne().defenseAfter());
        assertEquals(50, first.healthDamage());
        assertEquals(STARTING_HEALTH - 50, first.healthTwoAfter());
        assertNull(first.replacementOne());
        assertEquals(new CardRef(1, "AA02", "Recruit"), first.replacementTwo());
        assertEquals("Veteran infligge 350 danni a Recruit. Recruit scende a 0 DEF.",
                first.narration().lines().findFirst().orElseThrow());

        TurnLog second = log.turns().get(1);
        assertEquals(RoundOutcome.DOUBLE_KNOCKOUT, second.outcome());
        assertEquals(0, second.healthDamage());
        assertEquals(new CardRef(1, "AA01", "Veteran"), second.replacementOne());
        assertNull(second.replacementTwo());
    }

    @Test
    void summarisesEveryCardThatFought() {
        MatchSummary summary = playVeteranAgainstRecruits().log().summary();

        assertEquals(MatchOutcome.PLAYER_ONE_WINS, summary.outcome());
        assertEquals(MatchEndReason.DECK_EXHAUSTED, summary.endReason());
        assertEquals(2, summary.totalTurns());
        assertEquals(List.of(
                new CardSummary(PlayerSide.ONE, 0, "AA01", 1, 2, 700, 500, 2),
                new CardSummary(PlayerSide.TWO, 0, "AA02", 1, 1, 300, 350, 0),
                new CardSummary(PlayerSide.TWO, 1, "AA02", 2, 1, 200, 350, 1)),
                summary.cards());
    }

    private MatchResult playVeteranAgainstRecruits() {
        FighterCard veteran = new FighterCard("AA01", "Veteran", Faction.AFRICAN, 400, 500);
        FighterCard recruit = new FighterCard("AA02", "Recruit", Faction.AFRICAN, 300, 350);
        BaseRules rules = new BaseRules(1, STARTING_HEALTH, 2, 5, 10);
        return engine.play(List.of(veteran, veteran), List.of(recruit, recruit), SEED, rules);
    }

    private static void assertLogIsConsistent(MatchResult result) {
        MatchLog log = result.log();
        int healthOne = STARTING_HEALTH;
        int healthTwo = STARTING_HEALTH;
        TurnLog previous = null;
        for (TurnLog turn : log.turns()) {
            assertFighterContinues(previous == null ? null : previous.fighterOne(),
                    previous == null ? null : previous.replacementOne(), turn.fighterOne(), log.deckOneOrder());
            assertFighterContinues(previous == null ? null : previous.fighterTwo(),
                    previous == null ? null : previous.replacementTwo(), turn.fighterTwo(), log.deckTwoOrder());
            assertEquals(turn.fighterOne().damageDealt(),
                    turn.fighterTwo().defenseBefore() - turn.fighterTwo().defenseAfter());
            assertEquals(turn.fighterTwo().damageDealt(),
                    turn.fighterOne().defenseBefore() - turn.fighterOne().defenseAfter());
            if (turn.outcome() == RoundOutcome.FIRST_KNOCKED_OUT) {
                healthOne = Math.max(0, healthOne - turn.healthDamage());
            } else if (turn.outcome() == RoundOutcome.SECOND_KNOCKED_OUT) {
                healthTwo = Math.max(0, healthTwo - turn.healthDamage());
            }
            assertEquals(healthOne, turn.healthOneAfter());
            assertEquals(healthTwo, turn.healthTwoAfter());
            assertFalse(turn.narration().isBlank());
            previous = turn;
        }
        assertEquals(result.healthOne(), healthOne);
        assertEquals(result.healthTwo(), healthTwo);
        assertEquals(result.turnsPlayed(), log.summary().totalTurns());
        assertEquals(log.turns().size(),
                log.summary().cards().stream().filter(card -> card.player() == PlayerSide.ONE)
                        .mapToInt(CardSummary::roundsPlayed).sum());
    }

    private static void assertFighterContinues(FighterLog previous, CardRef replacement, FighterLog current,
            List<String> deckOrder) {
        if (previous == null) {
            assertEquals(0, current.deckPosition());
        } else if (previous.defenseAfter() > 0) {
            assertEquals(previous.deckPosition(), current.deckPosition());
            assertEquals(previous.defenseAfter(), current.defenseBefore());
        } else {
            assertEquals(replacement.deckPosition(), current.deckPosition());
            assertEquals(previous.deckPosition() + 1, current.deckPosition());
        }
        assertEquals(deckOrder.get(current.deckPosition()), current.cardCode());
    }

    private static List<FighterCard> randomDeck(Random generator, String codePrefix) {
        Faction[] factions = Faction.values();
        return IntStream.range(0, 8)
                .mapToObj(index -> new FighterCard(codePrefix + index, "Fighter " + codePrefix + index,
                        factions[generator.nextInt(factions.length)],
                        1 + generator.nextInt(1000), generator.nextInt(1001)))
                .toList();
    }

    private static List<String> codes(List<FighterCard> deck) {
        return deck.stream().map(FighterCard::code).toList();
    }
}
