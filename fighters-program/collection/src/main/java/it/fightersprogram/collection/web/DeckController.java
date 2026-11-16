package it.fightersprogram.collection.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import it.fightersprogram.collection.DeckDetail;
import it.fightersprogram.collection.DeckRequest;
import it.fightersprogram.collection.DeckService;
import it.fightersprogram.collection.DeckSummary;
import it.fightersprogram.identity.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/decks")
@RequiredArgsConstructor
@Tag(name = "Decks")
public class DeckController {

    private final DeckService deckService;

    @GetMapping
    @Operation(summary = "Decks of the current user")
    public List<DeckSummary> list(@AuthenticationPrincipal Jwt token) {
        return deckService.findDecks(CurrentUser.id(token));
    }

    @GetMapping("/{deckId}")
    @Operation(summary = "A deck of the current user with its cards")
    public DeckDetail get(@AuthenticationPrincipal Jwt token, @PathVariable long deckId) {
        return deckService.findDeck(CurrentUser.id(token), deckId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Save a new deck; 422 with the violations if it breaks the rules")
    public DeckDetail create(@AuthenticationPrincipal Jwt token, @Valid @RequestBody DeckRequest request) {
        return deckService.create(CurrentUser.id(token), request);
    }

    @PutMapping("/{deckId}")
    @Operation(summary = "Replace name and cards of a deck; 422 with the violations if it breaks the rules")
    public DeckDetail update(@AuthenticationPrincipal Jwt token, @PathVariable long deckId,
            @Valid @RequestBody DeckRequest request) {
        return deckService.update(CurrentUser.id(token), deckId, request);
    }

    @DeleteMapping("/{deckId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a deck that is not waiting in a challenge")
    public void delete(@AuthenticationPrincipal Jwt token, @PathVariable long deckId) {
        deckService.delete(CurrentUser.id(token), deckId);
    }
}
