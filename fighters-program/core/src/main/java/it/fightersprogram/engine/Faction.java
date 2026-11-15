package it.fightersprogram.engine;

public enum Faction {
    EURO_ATLANTIC,
    EURASIAN,
    AFRICAN,
    PAN_AMERICAN,
    INDO_PACIFIC;

    public boolean hasAdvantageOver(Faction other) {
        return other == nextInCycle();
    }

    private Faction nextInCycle() {
        Faction[] cycle = values();
        return cycle[(ordinal() + 1) % cycle.length];
    }
}
