package it.fightersprogram.identity;

public record TokenResponse(String accessToken, long expiresIn) {
}
