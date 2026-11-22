package it.fightersprogram.matches.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.fightersprogram.engine.BaseRules;
import it.fightersprogram.matches.AdvancedModeProperties;
import it.fightersprogram.matches.GameRulesProperties;

@Configuration
@EnableConfigurationProperties({GameRulesProperties.class, AdvancedModeProperties.class})
class GameRulesConfiguration {

    @Bean
    BaseRules baseRules(GameRulesProperties properties) {
        return properties.toBaseRules();
    }
}
