package it.fightersprogram.engine;

/**
 * The base game mode: no speed, dodge, variable damage or critical hits.
 */
public final class BaseRuleSet implements RuleSet {

    private final BaseRules rules;

    public BaseRuleSet(BaseRules rules) {
        this.rules = rules;
    }

    @Override
    public GameMode mode() {
        return GameMode.BASE;
    }

    @Override
    public BaseRules rules() {
        return rules;
    }

    @Override
    public Duel startMatch(long seed) {
        return new RoundResolver(rules.factionBonusPercent());
    }
}