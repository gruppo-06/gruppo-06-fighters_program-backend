package it.fightersprogram.collection;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DeckEntry(@NotNull Long cardId, @Min(1) int copies) {
}
