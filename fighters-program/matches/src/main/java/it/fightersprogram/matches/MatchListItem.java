package it.fightersprogram.matches;

import java.time.OffsetDateTime;

public record MatchListItem(long id, String challengerNickname, String opponentNickname, String status,
        OffsetDateTime createdAt) {
}
