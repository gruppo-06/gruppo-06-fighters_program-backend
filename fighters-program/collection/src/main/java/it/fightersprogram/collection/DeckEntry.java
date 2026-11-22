package it.fightersprogram.collection;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DeckEntry(@NotNull Long cardId, @Min(1) int copies, List<String> subcards) {

    public DeckEntry(Long cardId, int copies) {
        this(cardId, copies, null);
    }

    public List<String> subcardsOrEmpty() {
        return subcards == null ? List.of() : subcards;
    }
}
