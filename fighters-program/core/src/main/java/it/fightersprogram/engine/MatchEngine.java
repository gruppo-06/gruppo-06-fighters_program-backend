package it.fightersprogram.engine;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Plays a base-mode match. End rules still provisional until SWD_EN01 (#11): a player who must replace a knocked-out
 * card with an empty deck loses (both at once: decided by health); at the turn limit the healthier player wins,
 * equal health is a draw.
 */
public final class MatchEngine {

    public MatchResult play(List<FighterCard> deckOne, List<FighterCard> deckTwo, long seed, BaseRules rules) {
        requireDeckSize(deckOne, rules, "one");
        requireDeckSize(deckTwo, rules, "two");
        DeckShuffler shuffler = new DeckShuffler(seed);
        Player one = new Player(shuffler.shuffle(deckOne), rules.startingHealth());
        Player two = new Player(shuffler.shuffle(deckTwo), rules.startingHealth());
        RoundResolver resolver = new RoundResolver(rules.factionBonusPercent());

        for (int turn = 1; turn <= rules.maxTurns(); turn++) {
            RoundResult lastRound = fightUntilKnockout(resolver, one, two);
            applyHealthDamage(lastRound, one, two);
            if (one.isDefeated() || two.isDefeated()) {
                return result(one.isDefeated() ? MatchOutcome.PLAYER_TWO_WINS : MatchOutcome.PLAYER_ONE_WINS,
                        MatchEndReason.HEALTH_DEPLETED, turn, one, two);
            }
            one.replaceIfKnockedOut();
            two.replaceIfKnockedOut();
            if (!one.hasCardInPlay() || !two.hasCardInPlay()) {
                return result(exhaustedDeckOutcome(one, two), MatchEndReason.DECK_EXHAUSTED, turn, one, two);
            }
        }
        return result(healthierPlayer(one, two), MatchEndReason.TURN_LIMIT, rules.maxTurns(), one, two);
    }

    private static RoundResult fightUntilKnockout(RoundResolver resolver, Player one, Player two) {
        RoundResult round;
        do {
            round = resolver.resolve(one.inPlay, two.inPlay);
            one.inPlay = new FighterInPlay(round.first().card(), round.first().defenseAfter());
            two.inPlay = new FighterInPlay(round.second().card(), round.second().defenseAfter());
        } while (round.outcome() == RoundOutcome.NO_KNOCKOUT);
        return round;
    }

    private static void applyHealthDamage(RoundResult round, Player one, Player two) {
        if (round.outcome() == RoundOutcome.FIRST_KNOCKED_OUT) {
            one.takeDamage(healthDamage(round.second(), round.first()));
        } else if (round.outcome() == RoundOutcome.SECOND_KNOCKED_OUT) {
            two.takeDamage(healthDamage(round.first(), round.second()));
        }
    }

    // Provisional convention until SWD_EN01 (#24): effective attack minus the loser's defense before the lethal hit.
    private static int healthDamage(FighterRound survivor, FighterRound loser) {
        return Math.max(0, survivor.effectiveAttack() - loser.defenseBefore());
    }

    private static MatchOutcome exhaustedDeckOutcome(Player one, Player two) {
        if (one.hasCardInPlay()) {
            return MatchOutcome.PLAYER_ONE_WINS;
        }
        if (two.hasCardInPlay()) {
            return MatchOutcome.PLAYER_TWO_WINS;
        }
        return healthierPlayer(one, two);
    }

    private static MatchOutcome healthierPlayer(Player one, Player two) {
        if (one.health == two.health) {
            return MatchOutcome.DRAW;
        }
        return one.health > two.health ? MatchOutcome.PLAYER_ONE_WINS : MatchOutcome.PLAYER_TWO_WINS;
    }

    private static MatchResult result(MatchOutcome outcome, MatchEndReason reason, int turns, Player one, Player two) {
        return new MatchResult(outcome, reason, turns, one.health, two.health);
    }

    private static void requireDeckSize(List<FighterCard> deck, BaseRules rules, String player) {
        if (deck.size() != rules.deckSize()) {
            throw new IllegalArgumentException("Deck of player " + player + " must have " + rules.deckSize()
                    + " cards, got " + deck.size());
        }
    }

    private static final class Player {

        private final Deque<FighterCard> drawPile;
        private FighterInPlay inPlay;
        private int health;

        Player(List<FighterCard> shuffledDeck, int startingHealth) {
            this.drawPile = new ArrayDeque<>(shuffledDeck);
            this.inPlay = FighterInPlay.enter(drawPile.removeFirst());
            this.health = startingHealth;
        }

        void takeDamage(int damage) {
            health = Math.max(0, health - damage);
        }

        boolean isDefeated() {
            return health == 0;
        }

        void replaceIfKnockedOut() {
            if (inPlay.remainingDefense() == 0) {
                inPlay = drawPile.isEmpty() ? null : FighterInPlay.enter(drawPile.removeFirst());
            }
        }

        boolean hasCardInPlay() {
            return inPlay != null;
        }
    }
}
