package it.fightersprogram.matches;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import it.fightersprogram.catalog.SubcardService;
import it.fightersprogram.catalog.SubcardView;
import it.fightersprogram.engine.AdvancedRuleSet;
import it.fightersprogram.engine.BaseRuleSet;
import it.fightersprogram.engine.BaseRules;
import it.fightersprogram.engine.Faction;
import it.fightersprogram.engine.FighterCard;
import it.fightersprogram.engine.GameMode;
import it.fightersprogram.engine.MatchEngine;
import it.fightersprogram.engine.MatchResult;
import it.fightersprogram.engine.RuleSet;
import it.fightersprogram.engine.StatModifier;

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
    private final SubcardService subcardService;
    private final MatchEngine engine = new MatchEngine();

    public MatchSimulator(BaseRules rules,
            @Value("${game.rules.advanced-faction-bonus}") boolean advancedFactionBonus,
            SubcardService subcardService) {
        this.rules = rules;
        this.advancedFactionBonus = advancedFactionBonus;
        this.subcardService = subcardService;
    }

    public MatchResult simulate(List<SnapshotCard> deckOne, List<SnapshotCard> deckTwo, long seed, GameMode mode) {
        return engine.play(toEngineDeck(deckOne, mode), toEngineDeck(deckTwo, mode), seed, ruleSet(mode));
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

    private List<FighterCard> toEngineDeck(List<SnapshotCard> deck, GameMode mode) {
        Map<String, SubcardView> subcards = mode == GameMode.ADVANCED
                ? subcardService.findByCodes(subcardCodes(deck))
                : Map.of();
        return deck.stream()
                .map(card -> new FighterCard(card.code(), card.name(), engineFaction(card.faction()), card.atk(),
                        card.def(), valueOrZero(card.vel()), valueOrZero(card.crt()))
                        .withModifiers(modifiers(card, subcards)))
                .toList();
    }

    private static List<String> subcardCodes(List<SnapshotCard> deck) {
        return deck.stream()
                .flatMap(card -> subcardsOf(card).stream())
                .distinct()
                .toList();
    }

    private static List<StatModifier> modifiers(SnapshotCard card, Map<String, SubcardView> subcards) {
        return subcardsOf(card).stream()
                .map(subcards::get)
                .filter(Objects::nonNull)
                .map(view -> new StatModifier(view.atk(), view.def(), view.vel(), view.crt()))
                .toList();
    }

    private static List<String> subcardsOf(SnapshotCard card) {
        return card.subcards() == null ? List.of() : card.subcards();
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
