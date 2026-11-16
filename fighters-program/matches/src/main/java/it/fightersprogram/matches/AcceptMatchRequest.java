package it.fightersprogram.matches;

import jakarta.validation.constraints.NotNull;

public record AcceptMatchRequest(@NotNull Long deckId) {
}
