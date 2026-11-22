package it.fightersprogram.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    public static final ZoneId GAME_ZONE = ZoneId.of("Europe/Rome");

    @Bean
    Clock clock() {
        return Clock.system(GAME_ZONE);
    }
}
