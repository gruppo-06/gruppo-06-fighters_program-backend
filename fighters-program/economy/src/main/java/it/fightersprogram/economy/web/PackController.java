package it.fightersprogram.economy.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.economy.PackService;
import it.fightersprogram.economy.PackView;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/packs")
@RequiredArgsConstructor
@Tag(name = "Packs")
public class PackController {

    private final PackService packService;

    @GetMapping
    @Operation(summary = "List all active packs")
    public List<PackView> list() {
        return packService.activePacks();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get details of an active pack")
    public PackView detail(@PathVariable long id) {
        return packService.activePack(id);
    }
}