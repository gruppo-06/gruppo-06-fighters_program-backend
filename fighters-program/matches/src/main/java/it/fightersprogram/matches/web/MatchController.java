package it.fightersprogram.matches.web;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.fightersprogram.common.web.PageResponse;
import it.fightersprogram.identity.CurrentUser;
import it.fightersprogram.matches.AcceptMatchRequest;
import it.fightersprogram.matches.CreateMatchRequest;
import it.fightersprogram.matches.MatchHistoryItem;
import it.fightersprogram.matches.MatchListItem;
import it.fightersprogram.matches.MatchReplay;
import it.fightersprogram.matches.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Tag(name = "Matches")
public class MatchController {

    private static final int MAX_PAGE_SIZE = 100;

    private final MatchService matchService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Challenge another player with one of your decks")
    public MatchListItem challenge(@AuthenticationPrincipal Jwt token, @Valid @RequestBody CreateMatchRequest request) {
        return matchService.challenge(CurrentUser.id(token), request);
    }

    @GetMapping("/incoming")
    @Operation(summary = "Challenges waiting for your answer")
    public List<MatchListItem> incoming(@AuthenticationPrincipal Jwt token) {
        return matchService.incoming(CurrentUser.id(token));
    }

    @GetMapping("/outgoing")
    @Operation(summary = "Challenges you sent, in any state")
    public List<MatchListItem> outgoing(@AuthenticationPrincipal Jwt token) {
        return matchService.outgoing(CurrentUser.id(token));
    }

    @GetMapping("/history")
    @Operation(summary = "Your finished matches, newest first, with your result")
    public PageResponse<MatchHistoryItem> history(@AuthenticationPrincipal Jwt token,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.from(matchService.history(CurrentUser.id(token), pageRequest));
    }

    @GetMapping("/{matchId}")
    @Operation(summary = "A challenge you take part in; finished ones include decks and the turn-by-turn log")
    public MatchReplay replay(@AuthenticationPrincipal Jwt token, @PathVariable long matchId) {
        return matchService.replay(CurrentUser.id(token), matchId);
    }

    @PostMapping("/{matchId}/accept")
    @Operation(summary = "Accept with one of your decks: the server shuffles, plays and stores the match")
    public MatchReplay accept(@AuthenticationPrincipal Jwt token, @PathVariable long matchId,
            @Valid @RequestBody AcceptMatchRequest request) {
        return matchService.accept(CurrentUser.id(token), matchId, request);
    }

    @PostMapping("/{matchId}/decline")
    @Operation(summary = "Decline a challenge you received")
    public MatchListItem decline(@AuthenticationPrincipal Jwt token, @PathVariable long matchId) {
        return matchService.decline(CurrentUser.id(token), matchId);
    }

    @PostMapping("/{matchId}/cancel")
    @Operation(summary = "Cancel a challenge you sent")
    public MatchListItem cancel(@AuthenticationPrincipal Jwt token, @PathVariable long matchId) {
        return matchService.cancel(CurrentUser.id(token), matchId);
    }
}
