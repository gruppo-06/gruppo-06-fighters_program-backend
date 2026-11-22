package it.fightersprogram.economy;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCreditRequest(
        @NotNull @Min(-100000) @Max(100000) Integer amount,
        @NotBlank @Size(max = 200) String reason) {
}
