package it.fightersprogram.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Plays a match and records it round by round. End rules still provisional until SWD_EN01 (#11): a player who must
 * replace a knocked-out card with an empty deck loses (both at once: decided by health); at the turn limit the
 * healthier player wins, equal health is a draw.
 */
public final class MatchEngine {

    private final TurnNarrator narrator = new TurnNarrator();

    public MatchResult play(List<FighterCard> deckOne, List<FighterCard> deckTwo, long seed, BaseRules rules) {
        return play(deckOne, deckTwo, seed, new BaseRuleSet(rules));
    }

    public MatchResult play(List<FighterCard> deckOne, List<FighterCard> deckTwo, long seed, RuleSet ruleSet) {
        BaseRules rules = ruleSet.rules();
        requireDeckSize(deckOne, rules, "one");
        requireDeckSize(deckTwo, rules, "two");
        DeckShuffler shuffler = new DeckShuffler(seed);
        PlayerState one = new PlayerState(PlayerSide.ONE, shuffler.shuffle(deckOne), rules.startingHealth());
        PlayerState two = new PlayerState(PlayerSide.TWO, shuffler.shuffle(deckTwo), rules.startingHealth());
        return new Match(seed, ruleSet, one, two).play();
    }

    private static void requireDeckSize(List<FighterCard> deck, BaseRules rules, String player) {
        if (deck.size() != rules.deckSize()) {
            throw new IllegalArgumentException("Deck of player " + player + " must have " + rules.deckSize()
                    + " cards, got " + deck.size());
        }
    }

    private final class Match {

        private final long seed;
        private final RuleSet ruleSet;
        private final BaseRules rules;
        private final PlayerState one;
        private final PlayerState two;
        private final RuleSet.Duel duel;
        private final List<TurnLog> turns = new ArrayList<>();

        Match(long seed, RuleSet ruleSet, PlayerState one, PlayerState two) {
            this.seed = seed;
            this.ruleSet = ruleSet;
            this.rules = ruleSet.rules();
            this.one = one;
            this.two = two;
            this.duel = ruleSet.startMatch(seed);
        }

        MatchResult play() {
            for (int turn = 1; turn <= rules.maxTurns(); turn++) {
                playTurn(turn);
                if (one.isDefeated() || two.isDefeated()) {
                    MatchOutcome winner = one.isDefeated() ? MatchOutcome.PLAYER_TWO_WINS : MatchOutcome.PLAYER_ONE_WINS;
                    return finish(winner, MatchEndReason.HEALTH_DEPLETED, turn);
                }
                if (!one.hasCardInPlay() || !two.hasCardInPlay()) {
                    return finish(exhaustedDeckOutcome(), MatchEndReason.DECK_EXHAUSTED, turn);
                }
            }
            return finish(healthierPlayer(), MatchEndReason.TURN_LIMIT, rules.maxTurns());
        }

        private void playTurn(int turn) {
            int roundNumber = 0;
            RoundResult round;
            do {
                roundNumber++;
                round = duel.resolve(one.inPlay(), two.inPlay());
                FighterLog fighterOne = one.recordRound(round.first(), round.second(), turn);
                FighterLog fighterTwo = two.recordRound(round.second(), round.first(), turn);
                int healthDamage = applyHealthDamage(round);
                boolean isMatchOver = one.isDefeated() || two.isDefeated();
                CardRef replacementOne = isMatchOver ? null : one.replaceIfKnockedOut();
                CardRef replacementTwo = isMatchOver ? null : two.replaceIfKnockedOut();
                TurnLog turnLog = new TurnLog(turn, roundNumber, fighterOne, fighterTwo, round.outcome(),
                        healthDamage, one.health(), two.health(), replacementOne, replacementTwo, "");
                turns.add(turnLog.withNarration(narrator.narrate(turnLog)));
            } while (round.outcome() == RoundOutcome.NO_KNOCKOUT);
        }

        private int applyHealthDamage(RoundResult round) {
            return switch (round.outcome()) {
                case FIRST_KNOCKED_OUT -> one.takeDamage(duel.healthDamage(round.second(), round.first()));
                case SECOND_KNOCKED_OUT -> two.takeDamage(duel.healthDamage(round.first(), round.second()));
                case DOUBLE_KNOCKOUT, NO_KNOCKOUT -> 0;
            };
        }

        private MatchOutcome exhaustedDeckOutcome() {
            if (one.hasCardInPlay()) {
                return MatchOutcome.PLAYER_ONE_WINS;
            }
            if (two.hasCardInPlay()) {
                return MatchOutcome.PLAYER_TWO_WINS;
            }
            return healthierPlayer();
        }

        private MatchOutcome healthierPlayer() {
            if (one.health() == two.health()) {
                return MatchOutcome.DRAW;
            }
            return one.health() > two.health() ? MatchOutcome.PLAYER_ONE_WINS : MatchOutcome.PLAYER_TWO_WINS;
        }

        private MatchResult finish(MatchOutcome outcome, MatchEndReason reason, int turnsPlayed) {
            List<CardSummary> cards = Stream.concat(one.cardSummaries(), two.cardSummaries()).toList();
            MatchSummary summary = new MatchSummary(outcome, reason, turnsPlayed, cards);
            MatchLog log = new MatchLog(seed, ruleSet.mode(), rules.rulesVersion(), one.deckOrder(), two.deckOrder(),
                    turns, summary);
            return new MatchResult(outcome, reason, turnsPlayed, one.health(), two.health(), log);
        }
    }

    private static final class PlayerState {

        private final PlayerSide side;
        private final List<FighterCard> deck;
        private final CardStatistics[] statistics;
        private int inPlayPosition;
        private FighterInPlay inPlay;
        private int health;

        PlayerState(PlayerSide side, List<FighterCard> shuffledDeck, int startingHealth) {
            this.side = side;
            this.deck = shuffledDeck;
            this.statistics = new CardStatistics[shuffledDeck.size()];
            this.inPlayPosition = 0;
            this.inPlay = FighterInPlay.enter(shuffledDeck.getFirst());
            this.health = startingHealth;
        }

        FighterInPlay inPlay() {
            return inPlay;
        }

        int health() {
            return health;
        }

        boolean isDefeated() {
            return health == 0;
        }

        boolean hasCardInPlay() {
            return inPlay != null;
        }

        FighterLog recordRound(FighterRound own, FighterRound opponent, int turn) {
            if (statistics[inPlayPosition] == null) {
                statistics[inPlayPosition] = new CardStatistics(turn);
            }
            statistics[inPlayPosition].record(own, opponent);
            inPlay = new FighterInPlay(own.card(), own.defenseAfter());
            FighterCard card = own.card();
            return new FighterLog(inPlayPosition, card.code(), card.name(), card.faction(), own.hasFactionAdvantage(),
                    card.attack(), own.effectiveAttack(), own.damageDealt(), own.defenseBefore(), own.defenseAfter(),
                    own.struckFirst(), own.dodged(), own.criticalHit());
        }

        int takeDamage(int damage) {
            int applied = Math.min(damage, health);
            health -= applied;
            return applied;
        }

        CardRef replaceIfKnockedOut() {
            if (inPlay.remainingDefense() > 0) {
                return null;
            }
            int nextPosition = inPlayPosition + 1;
            if (nextPosition == deck.size()) {
                inPlay = null;
                return null;
            }
            inPlayPosition = nextPosition;
            FighterCard next = deck.get(nextPosition);
            inPlay = FighterInPlay.enter(next);
            return new CardRef(nextPosition, next.code(), next.name());
        }

        List<String> deckOrder() {
            return deck.stream().map(FighterCard::code).toList();
        }

        Stream<CardSummary> cardSummaries() {
            List<CardSummary> summaries = new ArrayList<>();
            for (int position = 0; position < statistics.length; position++) {
                if (statistics[position] != null) {
                    summaries.add(statistics[position].summarise(side, position, deck.get(position).code()));
                }
            }
            return summaries.stream();
        }
    }

    private static final class CardStatistics {

        private final int enteredOnTurn;
        private int roundsPlayed;
        private int damageDealt;
        private int damageTaken;
        private int knockouts;

        CardStatistics(int enteredOnTurn) {
            this.enteredOnTurn = enteredOnTurn;
        }

        void record(FighterRound own, FighterRound opponent) {
            roundsPlayed++;
            damageDealt += own.damageDealt();
            damageTaken += own.defenseBefore() - own.defenseAfter();
            if (opponent.isKnockedOut()) {
                knockouts++;
            }
        }

        CardSummary summarise(PlayerSide side, int position, String code) {
            return new CardSummary(side, position, code, enteredOnTurn, roundsPlayed,
                    damageDealt, damageTaken, knockouts);
        }
    }
}
