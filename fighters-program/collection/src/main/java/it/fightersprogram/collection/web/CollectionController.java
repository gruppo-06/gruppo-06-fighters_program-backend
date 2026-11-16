package it.fightersprogram.collection.web;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.fightersprogram.catalog.CardFilter;
import it.fightersprogram.collection.CollectionEntry;
import it.fightersprogram.collection.CollectionService;
import it.fightersprogram.collection.FactionProgress;
import it.fightersprogram.common.web.PageResponse;
import it.fightersprogram.identity.CurrentUser;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/collection")
@RequiredArgsConstructor
@Tag(name = "Collection")
public class CollectionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CollectionService collectionService;

    @GetMapping
    @Operation(summary = "Cards owned by the current user, optionally filtered by faction, class and rarity")
    public PageResponse<CollectionEntry> list(
            @AuthenticationPrincipal Jwt token,
            @RequestParam(required = false) String faction,
            @RequestParam(name = "class", required = false) String fighterClass,
            @RequestParam(required = false) String rarity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.from(collectionService.findCollection(CurrentUser.id(token),
                new CardFilter(faction, fighterClass, rarity), pageRequest));
    }

    @GetMapping("/progress")
    @Operation(summary = "Distinct cards owned per faction, against the catalog total")
    public List<FactionProgress> progress(@AuthenticationPrincipal Jwt token) {
        return collectionService.progress(CurrentUser.id(token));
    }
}
