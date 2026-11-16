package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BaseRulesTest {

    @Test
    void acceptsValidValues() {
        assertDoesNotThrow(() -> new BaseRules(1, 3000, 20, 5, 50));
    }

    @Test
    void rejectsNonPositiveStartingHealth() {
        assertThrows(IllegalArgumentException.class, () -> new BaseRules(1, 0, 20, 5, 50));
    }

    @Test
    void rejectsNonPositiveDeckSize() {
        assertThrows(IllegalArgumentException.class, () -> new BaseRules(1, 3000, 0, 5, 50));
    }

    @Test
    void rejectsNegativeFactionBonus() {
        assertThrows(IllegalArgumentException.class, () -> new BaseRules(1, 3000, 20, -1, 50));
    }

    @Test
    void rejectsNonPositiveTurnLimit() {
        assertThrows(IllegalArgumentException.class, () -> new BaseRules(1, 3000, 20, 5, 0));
    }

    @Test
    void rejectsNonPositiveRulesVersion() {
        assertThrows(IllegalArgumentException.class, () -> new BaseRules(0, 3000, 20, 5, 50));
    }
}
