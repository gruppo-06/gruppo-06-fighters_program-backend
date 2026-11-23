package it.fightersprogram.identity.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.fightersprogram.identity.CurrentUser;
import it.fightersprogram.identity.FriendRequestCommand;
import it.fightersprogram.identity.FriendRequestView;
import it.fightersprogram.identity.FriendRequests;
import it.fightersprogram.identity.FriendService;
import it.fightersprogram.identity.FriendView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/friends")
@RequiredArgsConstructor
@Validated
@Tag(name = "Friends")
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    @Operation(summary = "List your friends, sorted by nickname")
    public List<FriendView> friends(@AuthenticationPrincipal Jwt token) {
        return friendService.friends(CurrentUser.id(token));
    }

    @GetMapping("/requests")
    @Operation(summary = "List the friend requests you received and sent, newest first")
    public FriendRequests requests(@AuthenticationPrincipal Jwt token) {
        return friendService.requests(CurrentUser.id(token));
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a friend request to another player by nickname")
    public FriendRequestView request(@AuthenticationPrincipal Jwt token,
            @Valid @RequestBody FriendRequestCommand command) {
        return friendService.request(CurrentUser.id(token), command.nickname());
    }

    @PostMapping("/requests/{requestId}/accept")
    @Operation(summary = "Accept a friend request you received")
    public FriendView accept(@AuthenticationPrincipal Jwt token, @PathVariable long requestId) {
        return friendService.accept(CurrentUser.id(token), requestId);
    }

    @PostMapping("/requests/{requestId}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Decline a friend request you received, or cancel one you sent")
    public void decline(@AuthenticationPrincipal Jwt token, @PathVariable long requestId) {
        friendService.decline(CurrentUser.id(token), requestId);
    }

    @DeleteMapping("/{nickname}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove an accepted friend")
    public void remove(@AuthenticationPrincipal Jwt token, @PathVariable String nickname) {
        friendService.remove(CurrentUser.id(token), nickname);
    }
}
