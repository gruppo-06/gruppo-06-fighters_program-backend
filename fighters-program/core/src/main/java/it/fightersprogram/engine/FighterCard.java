package it.fightersprogram.engine;

import java.util.Objects;

public record FighterCard(String code, String name, Faction faction, int attack, int defense, int speed, int critical) {

    public FighterCard {
        requireNotBlank(code, "code");
        requireNotBlank(name, "name");
        Objects.requireNonNull(faction, "faction");
        if (attack <= 0) {
            throw new IllegalArgumentException("Card " + code + " must have a positive attack, got " + attack);
        }
        if (defense < 0) {
            throw new IllegalArgumentException("Card " + code + " must have a non-negative defense, got " + defense);
        }
        requireStatInRange(speed, "speed", code);
        requireStatInRange(critical, "critical", code);
    }

    public FighterCard(String code, String name, Faction faction, int attack, int defense) {
        this(code, name, faction, attack, defense, 0, 0);
    }

    private static void requireStatInRange(int value, String field, String code) {
        if (value < 0 || value > 1000) {
            throw new IllegalArgumentException("Card " + code + " must have " + field + " between 0 and 1000, got "
                    + value);
        }
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Card " + field + " must not be blank");
        }
    }
}
