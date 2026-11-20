package it.fightersprogram.trading;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TradeCardRequest(@NotNull @Min(1) Long cardId, @Min(1) int quantity) {
}
