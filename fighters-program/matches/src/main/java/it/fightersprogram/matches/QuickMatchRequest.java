package it.fightersprogram.matches;

import jakarta.validation.constraints.NotNull;

import it.fightersprogram.engine.GameMode;

/**
 * @param mode null means the base mode
 */
public record QuickMatchRequest(@NotNull Long deckId, GameMode mode) {
}
