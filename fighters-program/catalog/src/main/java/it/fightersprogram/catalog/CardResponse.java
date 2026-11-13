package it.fightersprogram.catalog;

public record CardResponse(
        String code,
        String name,
        String country,
        String faction,
        String fighterClass,
        String rarity,
        int atk,
        int def,
        int vel,
        int crt,
        String flavorText,
        String imageFile) {
}
