package it.fightersprogram.catalog.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.catalog.AdminCardRequest;
import it.fightersprogram.catalog.AdminCardResponse;
import it.fightersprogram.catalog.AdminCardService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@Tag(name = "Admin cards")
public class AdminCardController {

    private final AdminCardService adminCardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a card managed by admins (ADMIN only)")
    public AdminCardResponse create(@Valid @RequestBody AdminCardRequest request) {
        return adminCardService.create(request);
    }

    @PutMapping("/{code}")
    @Operation(summary = "Edit a card; the importer will no longer overwrite it. \"active\": true reactivates it")
    public AdminCardResponse edit(@PathVariable String code, @Valid @RequestBody AdminCardRequest request) {
        return adminCardService.edit(code, request);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a card: it leaves the catalog and packs but stays in its owners' collections")
    public void deactivate(@PathVariable String code) {
        adminCardService.deactivate(code);
    }
}
