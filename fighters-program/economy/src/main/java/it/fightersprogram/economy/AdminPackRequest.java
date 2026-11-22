package it.fightersprogram.economy;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminPackRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 255) String description,
        @NotNull @Min(0) Integer price,
        @NotEmpty List<@Valid AdminPackContentRequest> contents,
        Boolean active) {
}
