package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class BaseRuleSetTest {

    private static final long SEED = 42L;
    private static final BaseRules RULES = new BaseRules(3, 1000, 3, 5, 10);

    private final MatchEngine engine = new MatchEngine();

    @Test
    void ruleSetOverloadPlaysTheSameMatchAsTheBaseRulesOverload() {
        List<FighterCard> deckOne = List.of(
                card("AA01", 400, 500),
                card("AA02", 450, 400),
                card("AA03", 350, 600));
        List<FighterCard> deckTwo = List.of(
                card("AA04", 300, 550),
                card("AA05", 500, 350),
                card("AA06", 250, 450));

        MatchResult viaBaseRules = engine.play(deckOne, deckTwo, SEED, RULES);
        MatchResult viaRuleSet = engine.play(deckOne, deckTwo, SEED, new BaseRuleSet(RULES));

        assertEquals(viaBaseRules, viaRuleSet);
    }

    @Test
    void baseRuleSetUsesTheBaseMode() {
        RuleSet ruleSet = new BaseRuleSet(RULES);

        assertEquals(GameMode.BASE, ruleSet.mode());
        assertEquals(RULES, ruleSet.rules());
    }

    private static FighterCard card(String code, int attack, int defense) {
        return new FighterCard(code, "Fighter " + code, Faction.AFRICAN, attack, defense);
    }
}