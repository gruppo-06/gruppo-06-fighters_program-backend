package it.fightersprogram.matches;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import it.fightersprogram.engine.AdvancedRuleSet;
import it.fightersprogram.engine.BaseRuleSet;
import it.fightersprogram.engine.BaseRules;
import it.fightersprogram.engine.Faction;
import it.fightersprogram.engine.FighterCard;
import it.fightersprogram.engine.GameMode;
import it.fightersprogram.engine.MatchEngine;
import it.fightersprogram.engine.MatchResult;
import it.fightersprogram.engine.RuleSet;

@Component
public class MatchSimulator {

    private static final Map<String, Faction> ENGINE_FACTIONS = Map.of(
            "Euroatlantica", Faction.EURO_ATLANTIC,
            "Eurasiatica", Faction.EURASIAN,
            "Africana", Faction.AFRICAN,
            "Panamericana", Faction.PAN_AMERICAN,
            "Indo-Pacifica", Faction.INDO_PACIFIC);

    private final BaseRules rules;
    private final boolean advancedFactionBonus;
    private final MatchEngine engine = new MatchEngine();

    public MatchSimulator(BaseRules rules,
            @Value("${game.rules.advanced-faction-bonus}") boolean advancedFactionBonus) {
        this.rules = rules;
        this.advancedFactionBonus = advancedFactionBonus;
    }

    public MatchResult simulate(List<SnapshotCard> deckOne, List<SnapshotCard> deckTwo, long seed, GameMode mode) {
        return engine.play(toEngineDeck(deckOne), toEngineDeck(deckTwo), seed, ruleSet(mode));
    }

    static Faction engineFaction(String catalogFaction) {
        Faction faction = ENGINE_FACTIONS.get(catalogFaction);
        if (faction == null) {
            throw new IllegalStateException("No engine faction for catalog faction " + catalogFaction);
        }
        return faction;
    }

    private RuleSet ruleSet(GameMode mode) {
        return switch (mode) {
            case BASE -> new BaseRuleSet(rules);
            case ADVANCED -> new AdvancedRuleSet(rules, advancedFactionBonus);
        };
    }

    private static List<FighterCard> toEngineDeck(List<SnapshotCard> deck) {
        return deck.stream()
                .map(card -> new FighterCard(card.code(), card.name(), engineFaction(card.faction()), card.atk(),
                        card.def(), valueOrZero(card.vel()), valueOrZero(card.crt())))
                .toList();
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
