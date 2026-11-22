package it.fightersprogram.matches;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import it.fightersprogram.engine.GameMode;

/**
 * @param mode null means the base mode
 */
public record CreateMatchRequest(@NotBlank String opponentNickname, @NotNull Long deckId, GameMode mode) {
}
