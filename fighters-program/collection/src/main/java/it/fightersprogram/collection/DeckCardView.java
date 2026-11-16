package it.fightersprogram.collection;

public record DeckCardView(long cardId, String code, String name, String faction, String fighterClass,
        String rarity, int atk, int def, String imageFile, int copies) {
}
