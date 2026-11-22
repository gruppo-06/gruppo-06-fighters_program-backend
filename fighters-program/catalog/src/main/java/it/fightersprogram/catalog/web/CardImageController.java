package it.fightersprogram.catalog.web;

import java.util.concurrent.TimeUnit;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.catalog.domain.CardImageStorage;
import it.fightersprogram.catalog.domain.CardImageStorage.StoredImage;
import it.fightersprogram.common.error.NotFoundException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Catalog")
public class CardImageController {

    private static final CacheControl CACHE = CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic();

    private final CatalogService catalogService;
    private final CardImageStorage storage;

    @GetMapping("/{code}/image")
    @Operation(summary = "Get the illustration of a card, e.g. EA01")
    public ResponseEntity<Resource> image(@PathVariable String code,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        String fileName = catalogService.imageFileOf(code)
                .orElseThrow(() -> new NotFoundException("No illustration for card: " + code));
        StoredImage image = storage.load(fileName)
                .orElseThrow(() -> new NotFoundException("Illustration file missing for card: " + code));
        String etag = "\"" + image.size() + "-" + image.lastModified() + "\"";
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).cacheControl(CACHE).eTag(etag).build();
        }
        return ResponseEntity.ok()
                .cacheControl(CACHE)
                .eTag(etag)
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.resource());
    }
}
