package it.fightersprogram.engine;

import java.util.List;
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

    public FighterCard withModifiers(List<StatModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return this;
        }

        int atkSum = 0;
        int defSum = 0;
        int velSum = 0;
        int crtSum = 0;

        for (StatModifier modifier : modifiers) {
            atkSum += modifier.atkPercent();
            defSum += modifier.defPercent();
            velSum += modifier.velPercent();
            crtSum += modifier.crtPercent();
        }

        int newAttack = clampAttack(Math.round(attack * (100 + atkSum) / 100.0f));
        int newDefense = clampStat(Math.round(defense * (100 + defSum) / 100.0f));
        int newSpeed = clampStat(Math.round(speed * (100 + velSum) / 100.0f));
        int newCritical = clampStat(Math.round(critical * (100 + crtSum) / 100.0f));

        return new FighterCard(code, name, faction, newAttack, newDefense, newSpeed, newCritical);
    }

    private static int clampAttack(int value) {
        return Math.max(1, Math.min(value, 1000));
    }

    private static int clampStat(int value) {
        return Math.max(0, Math.min(value, 1000));
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
