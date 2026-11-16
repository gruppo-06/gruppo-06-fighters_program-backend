package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class FactionTest {

    @ParameterizedTest
    @CsvSource({
            "EURO_ATLANTIC, EURASIAN",
            "EURASIAN, AFRICAN",
            "AFRICAN, PAN_AMERICAN",
            "PAN_AMERICAN, INDO_PACIFIC",
            "INDO_PACIFIC, EURO_ATLANTIC"
    })
    void followsTheAdvantageCycle(Faction attacker, Faction defender) {
        assertTrue(attacker.hasAdvantageOver(defender));
        assertFalse(defender.hasAdvantageOver(attacker));
    }

    @ParameterizedTest
    @EnumSource(Faction.class)
    void hasAdvantageOverExactlyOneFaction(Faction faction) {
        long dominatedFactions = Arrays.stream(Faction.values()).filter(faction::hasAdvantageOver).count();

        assertEquals(1, dominatedFactions);
    }

    @ParameterizedTest
    @EnumSource(Faction.class)
    void hasNoAdvantageOverItself(Faction faction) {
        assertFalse(faction.hasAdvantageOver(faction));
    }
}
