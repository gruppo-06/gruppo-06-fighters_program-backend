package it.fightersprogram.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCardRequest(
        @NotBlank @Size(max = 10) String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String country,
        @NotBlank String faction,
        @NotBlank String fighterClass,
        @NotBlank String rarity,
        @NotNull @Min(0) @Max(1000) Integer atk,
        @NotNull @Min(0) @Max(1000) Integer def,
        @NotNull @Min(0) @Max(1000) Integer vel,
        @NotNull @Min(0) @Max(1000) Integer crt,
        String flavorText,
        @Size(max = 255) String imageFile,
        Boolean active) {
}
