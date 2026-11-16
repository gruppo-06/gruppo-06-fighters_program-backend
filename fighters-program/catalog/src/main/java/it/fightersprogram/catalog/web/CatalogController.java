package it.fightersprogram.catalog.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.catalog.CardFilter;
import it.fightersprogram.catalog.CardResponse;
import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.common.web.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Catalog")
public class CatalogController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CatalogService catalogService;

    @GetMapping
    @Operation(summary = "List cards, optionally filtered by faction, class and rarity")
    public PageResponse<CardResponse> list(
            @RequestParam(required = false) String faction,
            @RequestParam(name = "class", required = false) String fighterClass,
            @RequestParam(required = false) String rarity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("code"));
        return PageResponse.from(catalogService.findCards(new CardFilter(faction, fighterClass, rarity), pageRequest));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get a card by its code, e.g. EA01")
    public CardResponse get(@PathVariable String code) {
        return catalogService.findByCode(code);
    }
}
