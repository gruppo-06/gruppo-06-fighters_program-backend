package it.fightersprogram.engine;

/**
 * A game mode: the values that set up a match and the rules that resolve it.
 */
public interface RuleSet {

    GameMode mode();

    BaseRules rules();

    /**
     * Starts a match. Called once per match, so the returned duel can keep per-match random state.
     */
    Duel startMatch(long seed);

    /**
     * Rules of a single match.
     */
    interface Duel {

        RoundResult resolve(FighterInPlay first, FighterInPlay second);

        int healthDamage(FighterRound survivor, FighterRound loser);
    }
}