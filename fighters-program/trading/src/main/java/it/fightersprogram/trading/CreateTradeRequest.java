package it.fightersprogram.trading;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateTradeRequest(
        @NotBlank String recipientNickname,
        @NotEmpty @Valid @UniqueTradeCards List<TradeCardRequest> offered,
        @NotEmpty @Valid @UniqueTradeCards List<TradeCardRequest> requested) {
}
