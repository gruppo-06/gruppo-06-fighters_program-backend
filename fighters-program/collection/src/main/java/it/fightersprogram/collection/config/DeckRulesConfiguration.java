package it.fightersprogram.collection.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import it.fightersprogram.collection.DeckRules;

@Configuration
@EnableConfigurationProperties(DeckRules.class)
class DeckRulesConfiguration {
}
