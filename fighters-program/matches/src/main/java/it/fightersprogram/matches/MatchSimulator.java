package it.fightersprogram.matches;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import it.fightersprogram.engine.BaseRules;
import it.fightersprogram.engine.Faction;
import it.fightersprogram.engine.FighterCard;
import it.fightersprogram.engine.MatchEngine;
import it.fightersprogram.engine.MatchResult;

@Component
public class MatchSimulator {

    private static final Map<String, Faction> ENGINE_FACTIONS = Map.of(
            "Euroatlantica", Faction.EURO_ATLANTIC,
            "Eurasiatica", Faction.EURASIAN,
            "Africana", Faction.AFRICAN,
            "Panamericana", Faction.PAN_AMERICAN,
            "Indo-Pacifica", Faction.INDO_PACIFIC);

    private final BaseRules rules;
    private final MatchEngine engine = new MatchEngine();

    public MatchSimulator(BaseRules rules) {
        this.rules = rules;
    }

    public MatchResult simulate(List<SnapshotCard> deckOne, List<SnapshotCard> deckTwo, long seed) {
        return engine.play(toEngineDeck(deckOne), toEngineDeck(deckTwo), seed, rules);
    }

    static Faction engineFaction(String catalogFaction) {
        Faction faction = ENGINE_FACTIONS.get(catalogFaction);
        if (faction == null) {
            throw new IllegalStateException("No engine faction for catalog faction " + catalogFaction);
        }
        return faction;
    }

    private static List<FighterCard> toEngineDeck(List<SnapshotCard> deck) {
        return deck.stream()
                .map(card -> new FighterCard(card.code(), card.name(), engineFaction(card.faction()), card.atk(),
                        card.def()))
                .toList();
    }
}
