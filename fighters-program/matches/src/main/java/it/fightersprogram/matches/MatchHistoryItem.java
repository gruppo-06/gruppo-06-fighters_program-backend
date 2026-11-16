package it.fightersprogram.matches;

import java.time.OffsetDateTime;

public record MatchHistoryItem(long matchId, String opponentNickname, String result, OffsetDateTime finishedAt) {
}
