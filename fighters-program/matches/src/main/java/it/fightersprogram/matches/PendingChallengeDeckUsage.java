package it.fightersprogram.matches;

import org.springframework.stereotype.Component;

import it.fightersprogram.collection.DeckUsage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class PendingChallengeDeckUsage implements DeckUsage {

    private final MatchService matchService;

    @Override
    public boolean isUsedInPendingChallenge(long deckId) {
        return matchService.isDeckInPendingChallenge(deckId);
    }
}
