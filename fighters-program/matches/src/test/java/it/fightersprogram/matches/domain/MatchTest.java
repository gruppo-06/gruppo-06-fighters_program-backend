package it.fightersprogram.matches.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import it.fightersprogram.common.error.BadRequestException;
import it.fightersprogram.common.error.ConflictException;
import it.fightersprogram.common.error.ForbiddenException;

class MatchTest {

    private static final long CHALLENGER = 1;
    private static final long OPPONENT = 2;
    private static final long OUTSIDER = 3;
    private static final long CHALLENGER_DECK = 10;
    private static final long OPPONENT_DECK = 20;

    @Test
    void aNewChallengeIsPending() {
        Match match = Match.challenge(CHALLENGER, OPPONENT, CHALLENGER_DECK);

        assertEquals(MatchStatus.PENDING, match.getStatus());
        assertNotNull(match.getCreatedAt());
        assertTrue(match.involves(CHALLENGER));
        assertTrue(match.involves(OPPONENT));
        assertFalse(match.involves(OUTSIDER));
    }

    @Test
    void nobodyCanChallengeThemselves() {
        assertThrows(BadRequestException.class, () -> Match.challenge(CHALLENGER, CHALLENGER, CHALLENGER_DECK));
    }

    @Test
    void finishingRecordsTheResult() {
        Match match = Match.challenge(CHALLENGER, OPPONENT, CHALLENGER_DECK);

        match.finish(OPPONENT, OPPONENT_DECK, OPPONENT, 42L, "{}", "[]", "[]");

        assertEquals(MatchStatus.FINISHED, match.getStatus());
        assertEquals(OPPONENT, match.getWinnerId());
        assertEquals(42L, match.getSeed());
        assertEquals(OPPONENT_DECK, match.getOpponentDeckId());
        assertNotNull(match.getFinishedAt());
    }

    @Test
    void aDrawHasNoWinner() {
        Match match = Match.challenge(CHALLENGER, OPPONENT, CHALLENGER_DECK);

        match.finish(OPPONENT, OPPONENT_DECK, null, 42L, "{}", "[]", "[]");

        assertNull(match.getWinnerId());
    }

    @Test
    void onlyTheOpponentCanAccept() {
        Match match = Match.challenge(CHALLENGER, OPPONENT, CHALLENGER_DECK);

        assertThrows(ForbiddenException.class, () -> match.requireAcceptableBy(CHALLENGER));
    }

    @Test
    void aClosedChallengeCannotBeAcceptedAgain() {
        Match match = Match.challenge(CHALLENGER, OPPONENT, CHALLENGER_DECK);
        match.decline(OPPONENT);

        assertThrows(ConflictException.class, () -> match.requireAcceptableBy(OPPONENT));
    }

    @Test
    void onlyTheOpponentCanDecline() {
        Match match = Match.challenge(CHALLENGER, OPPONENT, CHALLENGER_DECK);

        assertThrows(ForbiddenException.class, () -> match.decline(CHALLENGER));
        match.decline(OPPONENT);
        assertEquals(MatchStatus.DECLINED, match.getStatus());
    }

    @Test
    void onlyTheChallengerCanCancel() {
        Match match = Match.challenge(CHALLENGER, OPPONENT, CHALLENGER_DECK);

        assertThrows(ForbiddenException.class, () -> match.cancel(OPPONENT));
        match.cancel(CHALLENGER);
        assertEquals(MatchStatus.CANCELLED, match.getStatus());
    }

    @Test
    void aFinishedChallengeCannotBeCancelledOrDeclined() {
        Match match = Match.challenge(CHALLENGER, OPPONENT, CHALLENGER_DECK);
        match.finish(OPPONENT, OPPONENT_DECK, CHALLENGER, 42L, "{}", "[]", "[]");

        assertThrows(ConflictException.class, () -> match.cancel(CHALLENGER));
        assertThrows(ConflictException.class, () -> match.decline(OPPONENT));
    }

    @Test
    void theResultIsSeenFromEachPlayer() {
        Match match = Match.challenge(CHALLENGER, OPPONENT, CHALLENGER_DECK);
        match.finish(OPPONENT, OPPONENT_DECK, CHALLENGER, 42L, "{}", "[]", "[]");

        assertEquals(MatchResultForPlayer.WIN, match.resultFor(CHALLENGER));
        assertEquals(MatchResultForPlayer.LOSS, match.resultFor(OPPONENT));
        assertEquals(OPPONENT, match.otherPlayer(CHALLENGER));
        assertEquals(CHALLENGER, match.otherPlayer(OPPONENT));
    }
}
