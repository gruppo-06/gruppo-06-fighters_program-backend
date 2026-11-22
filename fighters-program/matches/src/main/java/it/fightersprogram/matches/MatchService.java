package it.fightersprogram.matches;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.CatalogCard;
import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.collection.DeckEntry;
import it.fightersprogram.collection.DeckService;
import it.fightersprogram.collection.DeckValidator;
import it.fightersprogram.collection.Violation;
import it.fightersprogram.common.error.BadRequestException;
import it.fightersprogram.common.error.ConflictException;
import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.common.error.UnprocessableException;
import it.fightersprogram.engine.GameMode;
import it.fightersprogram.engine.MatchLog;
import it.fightersprogram.engine.MatchOutcome;
import it.fightersprogram.engine.MatchResult;
import it.fightersprogram.identity.UserDirectory;
import it.fightersprogram.matches.domain.Match;
import it.fightersprogram.matches.domain.MatchRepository;
import it.fightersprogram.matches.domain.MatchStatus;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private static final TypeReference<List<SnapshotCard>> SNAPSHOT = new TypeReference<>() {
    };

    private final MatchRepository matchRepository;
    private final UserDirectory userDirectory;
    private final DeckService deckService;
    private final DeckValidator deckValidator;
    private final CatalogService catalogService;
    private final MatchSimulator simulator;
    private final AdvancedModeProperties advancedMode;
    private final JsonMapper jsonMapper;
    private final ApplicationEventPublisher events;
    private final SecureRandom seeds = new SecureRandom();

    @Transactional
    public MatchListItem challenge(long userId, CreateMatchRequest request) {
        GameMode mode = request.mode() == null ? GameMode.BASE : request.mode();
        if (mode == GameMode.ADVANCED && !advancedMode.enabled()) {
            throw new BadRequestException("The advanced mode is not enabled");
        }
        long opponentId = userDirectory.findIdByNickname(request.opponentNickname())
                .orElseThrow(() -> new NotFoundException("Player not found: " + request.opponentNickname()));
        Match match = Match.challenge(userId, opponentId, request.deckId(), mode);
        requireValidDeck(userId, deckService.cardsOf(userId, request.deckId()));
        return toListItem(matchRepository.save(match));
    }

    public List<MatchListItem> incoming(long userId) {
        return matchRepository.findByOpponentIdAndStatusOrderByCreatedAtDesc(userId, MatchStatus.PENDING).stream()
                .map(this::toListItem)
                .toList();
    }

    public List<MatchListItem> outgoing(long userId) {
        return matchRepository.findByChallengerIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional
    public MatchReplay accept(long userId, long matchId, AcceptMatchRequest request) {
        Match match = visibleMatch(userId, matchId);
        match.requireAcceptableBy(userId);
        List<DeckEntry> opponentCards = deckService.cardsOf(userId, request.deckId());
        requireValidDeck(userId, opponentCards);
        List<DeckEntry> challengerCards = challengerCards(match);

        List<SnapshotCard> challengerDeck = snapshot(challengerCards);
        List<SnapshotCard> opponentDeck = snapshot(opponentCards);
        long seed = seeds.nextLong();
        MatchResult result = simulator.simulate(challengerDeck, opponentDeck, seed, match.getMode());
        Long winnerId = winnerOf(match, result.outcome());

        match.finish(userId, request.deckId(), winnerId, seed, jsonMapper.writeValueAsString(result.log()),
                jsonMapper.writeValueAsString(challengerDeck), jsonMapper.writeValueAsString(opponentDeck));
        events.publishEvent(new MatchFinished(match.getId(), winnerId));
        return toReplay(match);
    }

    @Transactional
    public MatchListItem decline(long userId, long matchId) {
        Match match = visibleMatch(userId, matchId);
        match.decline(userId);
        return toListItem(match);
    }

    @Transactional
    public MatchListItem cancel(long userId, long matchId) {
        Match match = visibleMatch(userId, matchId);
        match.cancel(userId);
        return toListItem(match);
    }

    public MatchReplay replay(long userId, long matchId) {
        return toReplay(visibleMatch(userId, matchId));
    }

    public Page<MatchHistoryItem> history(long userId, Pageable pageable) {
        return matchRepository.findFinishedFor(userId, pageable)
                .map(match -> new MatchHistoryItem(match.getId(),
                        userDirectory.nicknameOf(match.otherPlayer(userId)),
                        match.resultFor(userId).name(), match.getFinishedAt()));
    }

    boolean isDeckInPendingChallenge(long deckId) {
        return matchRepository.existsByChallengerDeckIdAndStatus(deckId, MatchStatus.PENDING);
    }

    private Match visibleMatch(long userId, long matchId) {
        return matchRepository.findById(matchId)
                .filter(match -> match.involves(userId))
                .orElseThrow(() -> new NotFoundException("Challenge not found: " + matchId));
    }

    private List<DeckEntry> challengerCards(Match match) {
        List<DeckEntry> cards = deckService.cardsOf(match.getChallengerId(), match.getChallengerDeckId());
        if (!deckValidator.validate(match.getChallengerId(), cards).isEmpty()) {
            throw new ConflictException("The challenger's deck is no longer valid: the challenge must be cancelled");
        }
        return cards;
    }

    private void requireValidDeck(long userId, List<DeckEntry> cards) {
        List<Violation> violations = deckValidator.validate(userId, cards);
        if (!violations.isEmpty()) {
            throw new UnprocessableException("INVALID_DECK", "The deck breaks the game rules",
                    violations.stream().map(violation -> violation.code() + ": " + violation.message()).toList());
        }
    }

    private List<SnapshotCard> snapshot(List<DeckEntry> entries) {
        Map<Long, CatalogCard> cards = catalogService.findCardsByIds(entries.stream().map(DeckEntry::cardId).toList())
                .stream()
                .collect(Collectors.toMap(CatalogCard::id, Function.identity()));
        List<SnapshotCard> deck = new ArrayList<>();
        for (DeckEntry entry : entries) {
            CatalogCard card = cards.get(entry.cardId());
            for (int copy = 0; copy < entry.copies(); copy++) {
                deck.add(new SnapshotCard(card.code(), card.name(), card.faction(), card.rarity(), card.atk(),
                        card.def(), card.vel(), card.crt()));
            }
        }
        return deck;
    }

    private static Long winnerOf(Match match, MatchOutcome outcome) {
        return switch (outcome) {
            case PLAYER_ONE_WINS -> match.getChallengerId();
            case PLAYER_TWO_WINS -> match.getOpponentId();
            case DRAW -> null;
        };
    }

    private MatchListItem toListItem(Match match) {
        return new MatchListItem(match.getId(), userDirectory.nicknameOf(match.getChallengerId()),
                userDirectory.nicknameOf(match.getOpponentId()), match.getStatus().name(), match.getCreatedAt(),
                match.getMode().name());
    }

    private MatchReplay toReplay(Match match) {
        boolean isFinished = match.getStatus() == MatchStatus.FINISHED;
        return new MatchReplay(match.getId(), match.getStatus().name(),
                userDirectory.nicknameOf(match.getChallengerId()), userDirectory.nicknameOf(match.getOpponentId()),
                match.getWinnerId() == null ? null : userDirectory.nicknameOf(match.getWinnerId()),
                match.getCreatedAt(), match.getFinishedAt(),
                isFinished ? jsonMapper.readValue(match.getChallengerDeckSnapshot(), SNAPSHOT) : null,
                isFinished ? jsonMapper.readValue(match.getOpponentDeckSnapshot(), SNAPSHOT) : null,
                isFinished ? jsonMapper.readValue(match.getLog(), MatchLog.class) : null,
                match.getMode().name());
    }
}
