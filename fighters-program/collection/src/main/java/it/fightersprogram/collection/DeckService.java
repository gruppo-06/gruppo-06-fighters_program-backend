package it.fightersprogram.collection;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.CatalogCard;
import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.catalog.SubcardService;
import it.fightersprogram.catalog.SubcardView;
import it.fightersprogram.collection.domain.Deck;
import it.fightersprogram.collection.domain.DeckCard;
import it.fightersprogram.collection.domain.DeckRepository;
import it.fightersprogram.collection.domain.DeckSubcard;
import it.fightersprogram.common.error.ConflictException;
import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.common.error.UnprocessableException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeckService {

    private final DeckRepository deckRepository;
    private final DeckValidator deckValidator;
    private final DeckRules rules;
    private final CatalogService catalogService;
    private final SubcardService subcardService;
    private final ObjectProvider<DeckUsage> deckUsage;

    public List<DeckSummary> findDecks(long userId) {
        return deckRepository.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream()
                .map(deck -> new DeckSummary(deck.getId(), deck.getName(), deck.cardCount(), deck.getCreatedAt()))
                .toList();
    }

    public DeckDetail findDeck(long userId, long deckId) {
        return toDetail(ownedDeck(userId, deckId));
    }

    public List<DeckEntry> cardsOf(long userId, long deckId) {
        Deck deck = ownedDeck(userId, deckId);
        Map<Long, List<Short>> subcardIds = subcardIdsByCard(deck);
        Map<Short, SubcardView> subcards = subcardsOf(deck);
        return deck.getCards().stream()
                .map(card -> new DeckEntry(card.getCardId(), card.getCopies(),
                        subcardCodes(subcardIds.getOrDefault(card.getCardId(), List.of()), subcards)))
                .toList();
    }

    @Transactional
    public DeckDetail create(long userId, DeckRequest request) {
        if (deckRepository.countByUserId(userId) >= rules.maxPerUser()) {
            throw new ConflictException("You already have " + rules.maxPerUser() + " decks, the maximum allowed");
        }
        if (deckRepository.existsByUserIdAndName(userId, request.name())) {
            throw new ConflictException("You already have a deck named " + request.name());
        }
        requireValid(userId, request.cards());
        PersistedCards persisted = toPersisted(request.cards());
        return toDetail(deckRepository.save(new Deck(userId, request.name(), persisted.cards(), persisted.subcards())));
    }

    @Transactional
    public DeckDetail update(long userId, long deckId, DeckRequest request) {
        Deck deck = ownedDeck(userId, deckId);
        if (deckRepository.existsByUserIdAndNameAndIdNot(userId, request.name(), deckId)) {
            throw new ConflictException("You already have a deck named " + request.name());
        }
        requireValid(userId, request.cards());
        PersistedCards persisted = toPersisted(request.cards());
        deck.replace(request.name(), persisted.cards(), persisted.subcards());
        return toDetail(deck);
    }

    @Transactional
    public void delete(long userId, long deckId) {
        Deck deck = ownedDeck(userId, deckId);
        DeckUsage usage = deckUsage.getIfAvailable();
        if (usage != null && usage.isUsedInPendingChallenge(deckId)) {
            throw new ConflictException("The deck is used in a challenge that is still pending");
        }
        deckRepository.delete(deck);
    }

    private Deck ownedDeck(long userId, long deckId) {
        return deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new NotFoundException("Deck not found: " + deckId));
    }

    private void requireValid(long userId, List<DeckEntry> cards) {
        List<Violation> violations = deckValidator.validate(userId, cards);
        if (!violations.isEmpty()) {
            throw new UnprocessableException("INVALID_DECK", "The deck breaks the game rules",
                    violations.stream().map(violation -> violation.code() + ": " + violation.message()).toList());
        }
    }

    private PersistedCards toPersisted(List<DeckEntry> entries) {
        Set<String> codes = entries.stream()
                .flatMap(entry -> entry.subcardsOrEmpty().stream())
                .collect(Collectors.toSet());
        Map<String, Short> subcardIds = subcardService.findIdsByCodes(codes);
        List<DeckCard> cards = entries.stream()
                .map(entry -> new DeckCard(entry.cardId(), entry.copies()))
                .toList();
        List<DeckSubcard> subcards = entries.stream()
                .flatMap(entry -> entry.subcardsOrEmpty().stream()
                        .map(subcardIds::get)
                        .filter(Objects::nonNull)
                        .map(subcardId -> new DeckSubcard(entry.cardId(), subcardId)))
                .toList();
        return new PersistedCards(cards, subcards);
    }

    private DeckDetail toDetail(Deck deck) {
        Map<Long, CatalogCard> cards = catalogService
                .findCardsByIds(deck.getCards().stream().map(DeckCard::getCardId).toList()).stream()
                .collect(Collectors.toMap(CatalogCard::id, Function.identity()));
        Map<Long, List<Short>> subcardIds = subcardIdsByCard(deck);
        Map<Short, SubcardView> subcards = subcardsOf(deck);
        List<DeckCardView> views = deck.getCards().stream()
                .map(deckCard -> {
                    CatalogCard card = cards.get(deckCard.getCardId());
                    List<Short> cardSubcardIds = subcardIds.getOrDefault(deckCard.getCardId(), List.of());
                    return new DeckCardView(card.id(), card.code(), card.name(), card.faction(), card.fighterClass(),
                            card.rarity(), card.atk(), card.def(), card.imageFile(), deckCard.getCopies(),
                            subcardViews(cardSubcardIds, subcards));
                })
                .toList();
        return new DeckDetail(deck.getId(), deck.getName(), deck.getCreatedAt(), views);
    }

    private static Map<Long, List<Short>> subcardIdsByCard(Deck deck) {
        return deck.getSubcards().stream()
                .collect(Collectors.groupingBy(DeckSubcard::getCardId,
                        Collectors.mapping(DeckSubcard::getSubcardId, Collectors.toList())));
    }

    private Map<Short, SubcardView> subcardsOf(Deck deck) {
        Set<Short> ids = deck.getSubcards().stream()
                .map(DeckSubcard::getSubcardId)
                .collect(Collectors.toSet());
        return subcardService.findByIds(ids);
    }

    private static List<SubcardView> subcardViews(List<Short> ids, Map<Short, SubcardView> subcards) {
        return ids.stream()
                .map(subcards::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SubcardView::code))
                .toList();
    }

    private static List<String> subcardCodes(List<Short> ids, Map<Short, SubcardView> subcards) {
        return subcardViews(ids, subcards).stream()
                .map(SubcardView::code)
                .toList();
    }

    private record PersistedCards(List<DeckCard> cards, List<DeckSubcard> subcards) {
    }
}
