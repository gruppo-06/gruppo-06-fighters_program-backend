package it.fightersprogram.collection;

import java.util.List;

import it.fightersprogram.catalog.SubcardView;

public record DeckCardView(long cardId, String code, String name, String faction, String fighterClass,
        String rarity, int atk, int def, String imageFile, int copies, List<SubcardView> subcards) {
}
