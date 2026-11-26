package it.fightersprogram.cosmetics.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.common.web.PageResponse;
import it.fightersprogram.cosmetics.SkinService;
import it.fightersprogram.cosmetics.SkinView;
import it.fightersprogram.cosmetics.domain.SkinType;
import it.fightersprogram.identity.CurrentUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/skins")
@RequiredArgsConstructor
@Tag(name = "Cosmetics")
public class SkinController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SkinService skinService;

    @GetMapping
    @Operation(summary = "List active skins, optionally filtered by type and card code")
    public PageResponse<SkinView> list(
            @RequestParam(required = false) SkinType type,
            @RequestParam(required = false) String cardCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("code"));
        return PageResponse.from(skinService.findSkins(type, cardCode, pageRequest));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get an active skin by its code")
    public SkinView get(@PathVariable String code) {
        return skinService.findByCode(code);
    }

    @PostMapping("/{id}/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Purchase an active skin with credits")
    public SkinView purchase(@AuthenticationPrincipal Jwt token, @PathVariable long id) {
        return skinService.purchase(CurrentUser.id(token), id);
    }
}
