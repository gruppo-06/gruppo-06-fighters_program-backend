package it.fightersprogram.collection;

import java.time.OffsetDateTime;
import java.util.List;

public record DeckDetail(long id, String name, OffsetDateTime createdAt, List<DeckCardView> cards) {
}
