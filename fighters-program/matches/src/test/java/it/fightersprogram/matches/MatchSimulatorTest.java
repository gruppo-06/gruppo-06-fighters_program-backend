package it.fightersprogram.matches;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import it.fightersprogram.engine.BaseRules;
import it.fightersprogram.engine.Faction;
import it.fightersprogram.engine.MatchResult;

class MatchSimulatorTest {

    private static final BaseRules RULES = new BaseRules(1, 500, 10, 5, 20);

    private final MatchSimulator simulator = new MatchSimulator(RULES);

    @Test
    void theSameSeedReplaysTheSameMatch() {
        MatchResult first = simulator.simulate(deck("EA", "Euroatlantica"), deck("IP", "Indo-Pacifica"), 7L);
        MatchResult second = simulator.simulate(deck("EA", "Euroatlantica"), deck("IP", "Indo-Pacifica"), 7L);

        assertEquals(first, second);
    }

    @Test
    void aDifferentSeedShufflesDifferently() {
        MatchResult first = simulator.simulate(deck("EA", "Euroatlantica"), deck("IP", "Indo-Pacifica"), 7L);
        MatchResult second = simulator.simulate(deck("EA", "Euroatlantica"), deck("IP", "Indo-Pacifica"), 8L);

        assertNotEquals(first.log().deckOneOrder(), second.log().deckOneOrder());
    }

    @Test
    void mapsEveryCatalogFactionToTheEngine() {
        assertEquals(Faction.EURO_ATLANTIC, MatchSimulator.engineFaction("Euroatlantica"));
        assertEquals(Faction.EURASIAN, MatchSimulator.engineFaction("Eurasiatica"));
        assertEquals(Faction.AFRICAN, MatchSimulator.engineFaction("Africana"));
        assertEquals(Faction.PAN_AMERICAN, MatchSimulator.engineFaction("Panamericana"));
        assertEquals(Faction.INDO_PACIFIC, MatchSimulator.engineFaction("Indo-Pacifica"));
    }

    @Test
    void anUnknownFactionIsAnError() {
        assertThrows(IllegalStateException.class, () -> MatchSimulator.engineFaction("Atlantidea"));
    }

    private static List<SnapshotCard> deck(String prefix, String faction) {
        return IntStream.rangeClosed(1, 10)
                .mapToObj(index -> new SnapshotCard(prefix + "%02d".formatted(index), "Fighter " + index, faction,
                        "Comune", 150 + index * 7, 380 + index * 11))
                .toList();
    }
}
