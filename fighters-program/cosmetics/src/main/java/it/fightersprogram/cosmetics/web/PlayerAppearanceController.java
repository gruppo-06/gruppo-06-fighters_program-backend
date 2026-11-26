package it.fightersprogram.cosmetics.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.cosmetics.PlayerAppearance;
import it.fightersprogram.cosmetics.SkinService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@Tag(name = "Cosmetics")
public class PlayerAppearanceController {

    private final SkinService skinService;

    @GetMapping("/{nickname}/appearance")
    @Operation(summary = "Get a player's active cosmetic appearance")
    public PlayerAppearance appearance(@PathVariable String nickname) {
        return skinService.appearance(nickname);
    }
}
