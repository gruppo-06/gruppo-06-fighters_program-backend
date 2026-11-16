package it.fightersprogram.matches;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMatchRequest(@NotBlank String opponentNickname, @NotNull Long deckId) {
}
