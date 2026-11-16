package it.fightersprogram.collection;

import java.time.OffsetDateTime;

public record DeckSummary(long id, String name, int cardCount, OffsetDateTime createdAt) {
}
