package it.fightersprogram.matches;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import it.fightersprogram.engine.BaseRules;

class GameRulesPropertiesTest {

    @Test
    void bindsTheRulesFromTheApplicationProperties() {
        Map<String, String> properties = Map.of(
                "game.rules.version", "1",
                "game.rules.starting-health", "500",
                "game.rules.deck-size", "10",
                "game.rules.faction-bonus-percent", "5",
                "game.rules.max-turns", "20",
                "game.rules.advanced-faction-bonus", "true");

        GameRulesProperties rules = new Binder(new MapConfigurationPropertySource(properties))
                .bind("game.rules", GameRulesProperties.class)
                .get();

        assertEquals(new GameRulesProperties(1, 500, 10, 5, 20, true), rules);
    }

    @Test
    void convertsToTheEngineRules() {
        GameRulesProperties rules = new GameRulesProperties(1, 500, 10, 5, 20, true);

        assertEquals(new BaseRules(1, 500, 10, 5, 20), rules.toBaseRules());
    }
}
