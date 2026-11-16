package it.fightersprogram.collection;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeckContents(@NotNull @Size(max = 100) List<@NotNull @Valid DeckEntry> cards) {
}
