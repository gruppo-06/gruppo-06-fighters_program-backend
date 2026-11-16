package it.fightersprogram.matches;

import org.springframework.boot.context.properties.ConfigurationProperties;

import it.fightersprogram.engine.BaseRules;

@ConfigurationProperties("game.rules")
public record GameRulesProperties(int version, int startingHealth, int deckSize, int factionBonusPercent,
        int maxTurns) {

    public BaseRules toBaseRules() {
        return new BaseRules(version, startingHealth, deckSize, factionBonusPercent, maxTurns);
    }
}
