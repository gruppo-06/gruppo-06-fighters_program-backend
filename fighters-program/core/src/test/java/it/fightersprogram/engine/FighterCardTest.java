package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FighterCardTest {

    @Test
    void acceptsValidStats() {
        assertDoesNotThrow(() -> new FighterCard("EA16", "Hana Kuroda", Faction.EURO_ATLANTIC, 281, 540));
    }

    @Test
    void rejectsBlankCode() {
        assertThrows(IllegalArgumentException.class,
                () -> new FighterCard(" ", "Hana Kuroda", Faction.EURO_ATLANTIC, 281, 540));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> new FighterCard("EA16", "", Faction.EURO_ATLANTIC, 281, 540));
    }

    @Test
    void rejectsMissingFaction() {
        assertThrows(NullPointerException.class, () -> new FighterCard("EA16", "Hana Kuroda", null, 281, 540));
    }

    @Test
    void rejectsNonPositiveAttackBecauseTwoHarmlessCardsWouldNeverEndATurn() {
        assertThrows(IllegalArgumentException.class,
                () -> new FighterCard("EA16", "Hana Kuroda", Faction.EURO_ATLANTIC, 0, 540));
    }

    @Test
    void rejectsNegativeDefense() {
        assertThrows(IllegalArgumentException.class,
                () -> new FighterCard("EA16", "Hana Kuroda", Faction.EURO_ATLANTIC, 281, -1));
    }
}
