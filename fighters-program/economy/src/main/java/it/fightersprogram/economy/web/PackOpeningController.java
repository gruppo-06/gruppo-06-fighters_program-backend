package it.fightersprogram.economy.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.common.error.ApiError;
import it.fightersprogram.common.error.BadRequestException;
import it.fightersprogram.economy.InsufficientCreditsException;
import it.fightersprogram.economy.PackOpeningResult;
import it.fightersprogram.economy.PackOpeningService;
import it.fightersprogram.identity.CurrentUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/packs")
@RequiredArgsConstructor
@Tag(name = "Packs")
public class PackOpeningController {

    private final PackOpeningService packOpeningService;

    @PostMapping("/{id}/open")
    @Operation(summary = "Open a pack; the Idempotency-Key header makes a retry return the same opening only once")
    public PackOpeningResult open(@AuthenticationPrincipal Jwt token, @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("The Idempotency-Key header is required");
        }
        return packOpeningService.open(CurrentUser.id(token), id, idempotencyKey);
    }

    @ExceptionHandler(InsufficientCreditsException.class)
    public ResponseEntity<ApiError> handleInsufficientCredits(InsufficientCreditsException exception) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(new ApiError("INSUFFICIENT_CREDITS", exception.getMessage(), List.of()));
    }
}
