package it.fightersprogram.economy.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.common.error.BadRequestException;
import it.fightersprogram.economy.AdminCreditRequest;
import it.fightersprogram.economy.AdminCreditResult;
import it.fightersprogram.economy.AdminCreditService;
import it.fightersprogram.identity.CurrentUser;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users/{nickname}/credits")
@RequiredArgsConstructor
@Tag(name = "Admin credits")
public class AdminCreditController {

    private final AdminCreditService adminCreditService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Grant credits (positive amount) or debit them (negative amount) to a player (ADMIN only); "
            + "the Idempotency-Key header makes a retry apply once")
    public AdminCreditResult grant(@AuthenticationPrincipal Jwt token, @PathVariable String nickname,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AdminCreditRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("The Idempotency-Key header is required");
        }
        return adminCreditService.grant(nickname, CurrentUser.id(token), idempotencyKey, request);
    }
}
