package it.fightersprogram.collection.domain;

import java.util.List;

public record DeckLine(long cardId, String cardName, String rarity, int copies, int ownedCopies,
        List<String> subcards) {

    public DeckLine(long cardId, String cardName, String rarity, int copies, int ownedCopies) {
        this(cardId, cardName, rarity, copies, ownedCopies, List.of());
    }

    public DeckLine {
        subcards = subcards == null ? List.of() : List.copyOf(subcards);
    }
}
