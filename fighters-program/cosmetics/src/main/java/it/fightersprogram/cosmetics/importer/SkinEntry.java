package it.fightersprogram.cosmetics.importer;

public record SkinEntry(
        String code,
        String name,
        String type,
        String cardCode,
        String rarity,
        int price,
        String imageFile) {
}
