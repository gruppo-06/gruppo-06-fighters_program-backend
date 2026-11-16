package it.fightersprogram.engine;

import java.util.Objects;

public record FighterInPlay(FighterCard card, int remainingDefense) {

    public FighterInPlay {
        Objects.requireNonNull(card, "card");
        if (remainingDefense < 0) {
            throw new IllegalArgumentException(
                    "Remaining defense of " + card.code() + " must not be negative, got " + remainingDefense);
        }
    }

    public static FighterInPlay enter(FighterCard card) {
        return new FighterInPlay(card, card.defense());
    }
}
