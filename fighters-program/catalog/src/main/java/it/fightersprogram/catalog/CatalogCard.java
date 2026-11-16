package it.fightersprogram.catalog;

public record CatalogCard(
        long id,
        String code,
        String name,
        String faction,
        String fighterClass,
        String rarity,
        int atk,
        int def,
        int vel,
        int crt,
        String imageFile) {
}
