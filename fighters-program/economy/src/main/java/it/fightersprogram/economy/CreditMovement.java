package it.fightersprogram.economy;

import java.time.OffsetDateTime;

public record CreditMovement(long id, int amount, CreditType type, String reference, OffsetDateTime createdAt) {
}
