package it.fightersprogram.catalog.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import it.fightersprogram.catalog.SubcardService;
import it.fightersprogram.catalog.SubcardView;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/subcards")
@RequiredArgsConstructor
@Tag(name = "Catalog")
public class SubcardController {

    private final SubcardService subcardService;

    @GetMapping
    @Operation(summary = "List the sub-cards that can be attached to deck cards")
    public List<SubcardView> list() {
        return subcardService.findAll();
    }
}
