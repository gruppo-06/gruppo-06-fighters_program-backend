package it.fightersprogram.economy.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.economy.AdminPackRequest;
import it.fightersprogram.economy.AdminPackService;
import it.fightersprogram.economy.AdminPackView;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/packs")
@RequiredArgsConstructor
@Tag(name = "Admin packs")
public class AdminPackController {

    private final AdminPackService adminPackService;

    @GetMapping
    @Operation(summary = "List all packs, also inactive ones, with their active flag (ADMIN only)")
    public List<AdminPackView> list() {
        return adminPackService.allPacks();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a pack (ADMIN only)")
    public AdminPackView create(@Valid @RequestBody AdminPackRequest request) {
        return adminPackService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a pack; \"active\": true reactivates it, \"active\": false deactivates it (ADMIN only)")
    public AdminPackView edit(@PathVariable long id, @Valid @RequestBody AdminPackRequest request) {
        return adminPackService.edit(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a pack: it leaves the shop but its openings are kept (ADMIN only)")
    public void deactivate(@PathVariable long id) {
        adminPackService.deactivate(id);
    }
}
