package it.fightersprogram.economy;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AdminPackContentRequest(
        @NotBlank String rarity,
        @Min(1) int cardCount) {
}
