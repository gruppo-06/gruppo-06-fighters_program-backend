package it.fightersprogram.economy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import it.fightersprogram.matches.MatchFinished;

import lombok.RequiredArgsConstructor;

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
            return;
        }
        creditService.credit(event.winnerId(), matchWin, CreditType.MATCH_WIN,
                "match-win:" + event.matchId(),
                "Match " + event.matchId() + " won");
    }
}
