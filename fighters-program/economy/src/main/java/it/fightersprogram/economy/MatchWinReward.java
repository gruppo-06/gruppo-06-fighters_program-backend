package it.fightersprogram.economy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import it.fightersprogram.matches.MatchFinished;

import lombok.RequiredArgsConstructor;

/**
 * Credits the winner of a finished match with the configured match-win amount.
 * Runs in a new transaction after the match transaction commits, so failures here
 * never roll back the match.
 */
@Component
@RequiredArgsConstructor
public class MatchWinReward {

    private final CreditService creditService;

    @Value("${game.credits.match-win}")
    private int matchWin;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMatchFinished(MatchFinished event) {
        if (event.winnerId() == null) {
            return; // draw, no reward
        }
        creditService.credit(event.winnerId(), matchWin, CreditType.MATCH_WIN,
                "match-win:" + event.matchId(),
                "Match " + event.matchId() + " won");
    }
}