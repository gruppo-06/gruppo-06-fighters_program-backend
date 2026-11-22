package it.fightersprogram.economy.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.economy.DailyBonusClaim;
import it.fightersprogram.economy.DailyBonusService;
import it.fightersprogram.economy.DailyBonusStatus;
import it.fightersprogram.identity.CurrentUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/credits/daily")
@RequiredArgsConstructor
@Tag(name = "Credits")
public class DailyBonusController {

    private final DailyBonusService dailyBonusService;

    @PostMapping
    @Operation(summary = "Claim today's daily bonus; consecutive days raise the amount")
    public DailyBonusClaim claim(@AuthenticationPrincipal Jwt token) {
        return dailyBonusService.claim(CurrentUser.id(token));
    }

    @GetMapping
    @Operation(summary = "Whether today's daily bonus is still claimable, your current streak and the next amount")
    public DailyBonusStatus status(@AuthenticationPrincipal Jwt token) {
        return dailyBonusService.status(CurrentUser.id(token));
    }
}
