package it.fightersprogram.matches.domain;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import it.fightersprogram.common.error.BadRequestException;
import it.fightersprogram.common.error.ConflictException;
import it.fightersprogram.common.error.ForbiddenException;
import it.fightersprogram.engine.GameMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "match")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long challengerId;

    private Long opponentId;

    private Long challengerDeckId;

    private Long opponentDeckId;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    @Enumerated(EnumType.STRING)
    private GameMode mode;

    private Long winnerId;

    private Long seed;

    @JdbcTypeCode(SqlTypes.JSON)
    private String log;

    @JdbcTypeCode(SqlTypes.JSON)
    private String challengerDeckSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    private String opponentDeckSnapshot;

    private OffsetDateTime createdAt;

    private OffsetDateTime finishedAt;

    public static Match challenge(long challengerId, long opponentId, long challengerDeckId) {
        return challenge(challengerId, opponentId, challengerDeckId, GameMode.BASE);
    }

    public static Match challenge(long challengerId, long opponentId, long challengerDeckId, GameMode mode) {
        if (challengerId == opponentId) {
            throw new BadRequestException("You cannot challenge yourself");
        }
        Match match = new Match();
        match.challengerId = challengerId;
        match.opponentId = opponentId;
        match.challengerDeckId = challengerDeckId;
        match.mode = mode;
        match.status = MatchStatus.PENDING;
        match.createdAt = OffsetDateTime.now();
        return match;
    }

    public boolean involves(long userId) {
        return challengerId == userId || opponentId == userId;
    }

    public long otherPlayer(long userId) {
        return challengerId == userId ? opponentId : challengerId;
    }

    public void requireAcceptableBy(long userId) {
        requireOpponent(userId, "Only the challenged player can accept");
        requirePending();
    }

    public void finish(long userId, long opponentDeck, Long winner, long matchSeed, String matchLog,
            String challengerSnapshot, String opponentSnapshot) {
        requireAcceptableBy(userId);
        this.opponentDeckId = opponentDeck;
        this.winnerId = winner;
        this.seed = matchSeed;
        this.log = matchLog;
        this.challengerDeckSnapshot = challengerSnapshot;
        this.opponentDeckSnapshot = opponentSnapshot;
        this.status = MatchStatus.FINISHED;
        this.finishedAt = OffsetDateTime.now();
    }

    public void decline(long userId) {
        requireOpponent(userId, "Only the challenged player can decline");
        requirePending();
        status = MatchStatus.DECLINED;
    }

    public void cancel(long userId) {
        if (challengerId != userId) {
            throw new ForbiddenException("Only the challenger can cancel");
        }
        requirePending();
        status = MatchStatus.CANCELLED;
    }

    public MatchResultForPlayer resultFor(long userId) {
        if (winnerId == null) {
            return MatchResultForPlayer.DRAW;
        }
        return winnerId == userId ? MatchResultForPlayer.WIN : MatchResultForPlayer.LOSS;
    }

    private void requireOpponent(long userId, String message) {
        if (opponentId != userId) {
            throw new ForbiddenException(message);
        }
    }

    private void requirePending() {
        if (status != MatchStatus.PENDING) {
            throw new ConflictException("The challenge is already " + status.name().toLowerCase());
        }
    }
}
