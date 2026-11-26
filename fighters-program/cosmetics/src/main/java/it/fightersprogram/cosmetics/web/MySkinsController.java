package it.fightersprogram.cosmetics.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.cosmetics.OwnedSkinView;
import it.fightersprogram.cosmetics.SkinService;
import it.fightersprogram.identity.CurrentUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/skins")
@RequiredArgsConstructor
@Tag(name = "Cosmetics")
public class MySkinsController {

    private final SkinService skinService;

    @GetMapping
    @Operation(summary = "List cosmetic skins owned by the authenticated user")
    public List<OwnedSkinView> list(@AuthenticationPrincipal Jwt token) {
        return skinService.findOwnedSkins(CurrentUser.id(token));
    }
}
