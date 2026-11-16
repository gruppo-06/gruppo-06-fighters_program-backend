package it.fightersprogram.collection.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.fightersprogram.collection.DeckContents;
import it.fightersprogram.collection.DeckValidationResult;
import it.fightersprogram.collection.DeckValidator;
import it.fightersprogram.identity.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/decks")
@RequiredArgsConstructor
@Tag(name = "Decks")
public class DeckValidationController {

    private final DeckValidator deckValidator;

    @PostMapping("/validate")
    @Operation(summary = "Check a deck against the rules without saving it")
    public DeckValidationResult validate(@AuthenticationPrincipal Jwt token, @Valid @RequestBody DeckContents deck) {
        return DeckValidationResult.of(deckValidator.validate(CurrentUser.id(token), deck.cards()));
    }
}
