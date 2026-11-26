package it.fightersprogram.cosmetics.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.cosmetics.OwnedSkinView;
import it.fightersprogram.cosmetics.SelectSkinRequest;
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

    @PutMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Select or reset the current user's avatar skin")
    public void selectAvatar(@AuthenticationPrincipal Jwt token, @RequestBody SelectSkinRequest request) {
        skinService.selectAvatar(CurrentUser.id(token), request.skinId());
    }

    @PutMapping("/cards/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Select or reset a card's variant skin")
    public void selectCardVariant(@AuthenticationPrincipal Jwt token, @PathVariable long cardId,
            @RequestBody SelectSkinRequest request) {
        skinService.selectCardVariant(CurrentUser.id(token), cardId, request.skinId());
    }
}
