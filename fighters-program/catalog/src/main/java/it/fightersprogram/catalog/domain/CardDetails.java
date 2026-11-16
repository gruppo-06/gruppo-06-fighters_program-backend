package it.fightersprogram.catalog.domain;

public record CardDetails(
        String name,
        String country,
        Faction faction,
        FighterClass fighterClass,
        Rarity rarity,
        int atk,
        int def,
        int vel,
        int crt,
        String flavorText,
        String imageFile) {
}
