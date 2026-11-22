package it.fightersprogram.engine;

public record StatModifier(int atkPercent, int defPercent, int velPercent, int crtPercent) {

    public StatModifier {
        validatePercent(atkPercent, "atkPercent");
        validatePercent(defPercent, "defPercent");
        validatePercent(velPercent, "velPercent");
        validatePercent(crtPercent, "crtPercent");
    }

    private static void validatePercent(int value, String field) {
        if (value < -50 || value > 50) {
            throw new IllegalArgumentException(field + " must be between -50 and 50, got " + value);
        }
    }
}