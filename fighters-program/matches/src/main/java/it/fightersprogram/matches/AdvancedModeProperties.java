package it.fightersprogram.matches;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("game.modes.advanced")
public record AdvancedModeProperties(boolean enabled) {
}
