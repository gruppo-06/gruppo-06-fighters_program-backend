package it.fightersprogram.identity.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.fightersprogram.identity.CurrentUser;
import it.fightersprogram.identity.UserDirectory;
import it.fightersprogram.identity.UserProfile;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Profile")
public class MeController {

    private final UserDirectory userDirectory;

    @GetMapping
    @Operation(summary = "Profile of the authenticated user")
    public UserProfile me(@AuthenticationPrincipal Jwt token) {
        return userDirectory.profileOf(CurrentUser.id(token));
    }
}
