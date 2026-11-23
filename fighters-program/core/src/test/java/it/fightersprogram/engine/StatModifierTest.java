package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class StatModifierTest {

    @Test
    void acceptsValidPercentages() {
        var modifier = new StatModifier(10, -5, 0, 20);
        assertEquals(10, modifier.atkPercent());
        assertEquals(-5, modifier.defPercent());
        assertEquals(0, modifier.velPercent());
        assertEquals(20, modifier.crtPercent());
    }

    @Test
    void rejectsAtkPercentAboveMax() {
        assertThrows(IllegalArgumentException.class, () -> new StatModifier(51, 0, 0, 0));
    }

    @Test
    void rejectsAtkPercentBelowMin() {
        assertThrows(IllegalArgumentException.class, () -> new StatModifier(-51, 0, 0, 0));
    }

    @Test
    void rejectsDefPercentAboveMax() {
        assertThrows(IllegalArgumentException.class, () -> new StatModifier(0, 51, 0, 0));
    }

    @Test
    void rejectsDefPercentBelowMin() {
        assertThrows(IllegalArgumentException.class, () -> new StatModifier(0, -51, 0, 0));
    }

    @Test
    void rejectsVelPercentAboveMax() {
        assertThrows(IllegalArgumentException.class, () -> new StatModifier(0, 0, 51, 0));
    }

    @Test
    void rejectsVelPercentBelowMin() {
        assertThrows(IllegalArgumentException.class, () -> new StatModifier(0, 0, -51, 0));
    }

    @Test
    void rejectsCrtPercentAboveMax() {
        assertThrows(IllegalArgumentException.class, () -> new StatModifier(0, 0, 0, 51));
    }

    @Test
    void rejectsCrtPercentBelowMin() {
        assertThrows(IllegalArgumentException.class, () -> new StatModifier(0, 0, 0, -51));
    }

    @Test
    void singleModifierAppliedToCard() {
        var card = new FighterCard("TEST1", "Test Card", Faction.EURO_ATLANTIC, 250, 300, 200, 150);
        var modifier = new StatModifier(10, 5, -5, 20);

        var modified = card.withModifiers(List.of(modifier));

        // atk: 250 * 1.10 = 275
        // def: 300 * 1.05 = 315
        // vel: 200 * 0.95 = 190
        // crt: 150 * 1.20 = 180
        assertEquals(275, modified.attack());
        assertEquals(315, modified.defense());
        assertEquals(190, modified.speed());
        assertEquals(180, modified.critical());
        assertEquals(card.code(), modified.code());
        assertEquals(card.name(), modified.name());
        assertEquals(card.faction(), modified.faction());
    }

    @Test
    void threeModifiersStackAdditively() {
        var card = new FighterCard("TEST2", "Test Card 2", Faction.INDO_PACIFIC, 200, 250, 300, 100);
        var modifiers = List.of(
                new StatModifier(10, 0, 0, 0),
                new StatModifier(5, -5, 0, 0),
                new StatModifier(0, 10, -10, 5)
        );

        var modified = card.withModifiers(modifiers);

        // atk: 200 * 1.15 = 230
        // def: 250 * 1.05 = 262.5 -> 263 (round half up)
        // vel: 300 * 0.90 = 270
        // crt: 100 * 1.05 = 105
        assertEquals(230, modified.attack());
        assertEquals(263, modified.defense());
        assertEquals(270, modified.speed());
        assertEquals(105, modified.critical());
    }

    @Test
    void negativeResultsClampedAttackNeverBelowOne() {
        var card = new FighterCard("TEST3", "Test Card 3", Faction.EURASIAN, 10, 10, 10, 10);
        var modifier = new StatModifier(-50, -50, -50, -50);

        var modified = card.withModifiers(List.of(modifier));

        // atk: 10 * 0.50 = 5 -> clamped to at least 1 (but 5 > 1, so 5)
        // def: 10 * 0.50 = 5
        // vel: 10 * 0.50 = 5
        // crt: 10 * 0.50 = 5
        assertEquals(5, modified.attack());
        assertEquals(5, modified.defense());
        assertEquals(5, modified.speed());
        assertEquals(5, modified.critical());
    }

    @Test
    void attackClampedToMinimumOne() {
        var card = new FighterCard("TEST4", "Test Card 4", Faction.AFRICAN, 1, 100, 100, 100);
        var modifier = new StatModifier(-50, 0, 0, 0);

        var modified = card.withModifiers(List.of(modifier));

        // atk: 1 * 0.50 = 0.5 -> rounded to 1 (half up), clamped to minimum 1
        assertEquals(1, modified.attack());
    }

    @Test
    void defenseSpeedCriticalClampedToZero() {
        var card = new FighterCard("TEST5", "Test Card 5", Faction.PAN_AMERICAN, 100, 1, 1, 1);
        var modifier = new StatModifier(0, -50, -50, -50);

        var modified = card.withModifiers(List.of(modifier));

        // def: 1 * 0.50 = 0.5 -> rounded to 1 (half up)
        // vel: 1 * 0.50 = 0.5 -> rounded to 1
        // crt: 1 * 0.50 = 0.5 -> rounded to 1
        assertEquals(1, modified.defense());
        assertEquals(1, modified.speed());
        assertEquals(1, modified.critical());
    }

    @Test
    void roundingHalfUp() {
        // 245 * 1.12 = 274.4 -> 274
        var card1 = new FighterCard("TEST6", "Test Card 6", Faction.EURO_ATLANTIC, 245, 200, 200, 200);
        var mod1 = new StatModifier(12, 0, 0, 0);
        var modified1 = card1.withModifiers(List.of(mod1));
        assertEquals(274, modified1.attack());

        // 250 * 1.10 = 275.0 -> 275
        var card2 = new FighterCard("TEST7", "Test Card 7", Faction.EURO_ATLANTIC, 250, 200, 200, 200);
        var mod2 = new StatModifier(10, 0, 0, 0);
        var modified2 = card2.withModifiers(List.of(mod2));
        assertEquals(275, modified2.attack());

        // 100 * 1.33 = 133.0 -> 133
        var card3 = new FighterCard("TEST8", "Test Card 8", Faction.EURO_ATLANTIC, 100, 200, 200, 200);
        var mod3 = new StatModifier(33, 0, 0, 0);
        var modified3 = card3.withModifiers(List.of(mod3));
        assertEquals(133, modified3.attack());
    }

    @Test
    void emptyModifierListReturnsEqualCard() {
        var card = new FighterCard("TEST9", "Test Card 9", Faction.INDO_PACIFIC, 300, 400, 250, 150);
        var modified = card.withModifiers(List.of());

        assertEquals(card, modified);
    }

    @Test
    void statsClampedToMaximum1000() {
        var card = new FighterCard("TEST10", "Test Card 10", Faction.EURASIAN, 900, 900, 900, 900);
        var modifier = new StatModifier(20, 20, 20, 20);

        var modified = card.withModifiers(List.of(modifier));

        // All stats would exceed 1000, should be clamped
        assertEquals(1000, modified.attack());
        assertEquals(1000, modified.defense());
        assertEquals(1000, modified.speed());
        assertEquals(1000, modified.critical());
    }
}