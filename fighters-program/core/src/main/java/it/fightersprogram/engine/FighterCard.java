package it.fightersprogram.engine;

import java.util.Objects;

public record FighterCard(String code, String name, Faction faction, int attack, int defense) {

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
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Card " + field + " must not be blank");
        }
    }
}
