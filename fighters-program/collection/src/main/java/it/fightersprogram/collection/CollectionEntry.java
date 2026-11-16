package it.fightersprogram.collection;

public record CollectionEntry(
        long cardId,
        String code,
        String name,
        String faction,
        String fighterClass,
        String rarity,
        int atk,
        int def,
        int vel,
        int crt,
        String imageFile,
        int quantity) {
}
