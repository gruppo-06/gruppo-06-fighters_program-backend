package it.fightersprogram.identity.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("identity.jwt")
public record JwtProperties(String secret, Duration expiry) {

    private static final int MIN_SECRET_LENGTH = 32;

    public JwtProperties {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "identity.jwt.secret (env JWT_SECRET) must be at least " + MIN_SECRET_LENGTH + " characters");
        }
        if (expiry == null || expiry.isNegative() || expiry.isZero()) {
            throw new IllegalStateException("identity.jwt.expiry must be a positive duration");
        }
    }
}
