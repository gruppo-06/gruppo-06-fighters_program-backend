package it.fightersprogram.economy.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.economy.CreditService;
import it.fightersprogram.economy.CreditSummary;
import it.fightersprogram.identity.CurrentUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/credits")
@RequiredArgsConstructor
@Tag(name = "Credits")
public class CreditController {

    private final CreditService creditService;

    @GetMapping
    @Operation(summary = "Your credit balance and your latest 20 movements, newest first")
    public CreditSummary summary(@AuthenticationPrincipal Jwt token) {
        return creditService.summary(CurrentUser.id(token));
    }
}
