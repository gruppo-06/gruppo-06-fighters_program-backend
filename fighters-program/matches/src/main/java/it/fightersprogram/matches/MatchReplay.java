package it.fightersprogram.matches;

import java.time.OffsetDateTime;
import java.util.List;

import it.fightersprogram.engine.MatchLog;

/**
 * @param log null while the challenge is not finished
 * @param mode {@code BASE} or {@code ADVANCED}
 */
public record MatchReplay(
        long id,
        String status,
        String challengerNickname,
        String opponentNickname,
        String winnerNickname,
        OffsetDateTime createdAt,
        OffsetDateTime finishedAt,
        List<SnapshotCard> challengerDeck,
        List<SnapshotCard> opponentDeck,
        MatchLog log,
        String mode) {
}
