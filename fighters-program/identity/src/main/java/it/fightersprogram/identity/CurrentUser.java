package it.fightersprogram.identity;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static long id(Jwt token) {
        return Long.parseLong(token.getSubject());
    }
}
