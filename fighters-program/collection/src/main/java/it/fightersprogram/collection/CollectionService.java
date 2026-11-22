package it.fightersprogram.collection;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.CardFilter;
import it.fightersprogram.catalog.CatalogCard;
import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.common.error.UnprocessableException;
import it.fightersprogram.collection.domain.UserCard;
import it.fightersprogram.collection.domain.UserCardId;
import it.fightersprogram.collection.domain.UserCardRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {

    private final UserCardRepository userCardRepository;
    private final CatalogService catalogService;

    public Page<CollectionEntry> findCollection(long userId, CardFilter filter, Pageable pageable) {
        List<CollectionEntry> entries = ownedEntries(userId).stream()
                .filter(matches(filter))
                .sorted(Comparator.comparing(CollectionEntry::code))
                .toList();
        int from = (int) Math.min(pageable.getOffset(), entries.size());
        int to = Math.min(from + pageable.getPageSize(), entries.size());
        return new PageImpl<>(entries.subList(from, to), pageable, entries.size());
    }

    public List<FactionProgress> progress(long userId) {
        List<CatalogCard> catalog = catalogService.findAllCards();
        Map<String, Long> totalByFaction = catalog.stream()
                .collect(Collectors.groupingBy(CatalogCard::faction, Collectors.counting()));
        Map<String, Long> ownedByFaction = ownedEntries(userId).stream()
                .collect(Collectors.groupingBy(CollectionEntry::faction, Collectors.counting()));
        return catalogService.factionNames().stream()
                .map(faction -> new FactionProgress(faction,
                        ownedByFaction.getOrDefault(faction, 0L).intValue(),
                        totalByFaction.getOrDefault(faction, 0L).intValue()))
                .toList();
    }

    public Map<Long, Integer> ownedQuantities(long userId) {
        return userCardRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserCard::getCardId, UserCard::getQuantity));
    }

    @Transactional
    public void grant(long userId, long cardId, int copies) {
        if (copies < 1) {
            throw new IllegalArgumentException("Copies must be at least 1, got " + copies);
        }
        userCardRepository.addCopiesAtomically(userId, cardId, copies);
    }

    @Transactional
    public void transfer(List<CardTransfer> transfers) {
        if (transfers.isEmpty()) {
            return;
        }

        Map<UserCardId, Integer> outgoing = new HashMap<>();
        Map<UserCardId, Integer> incoming = new HashMap<>();
        List<Long> userIds = transfers.stream()
                .flatMap(transfer -> Stream.of(transfer.fromUserId(), transfer.toUserId()))
                .distinct().sorted().toList();
        for (CardTransfer transfer : transfers) {
            if (transfer.quantity() < 1) {
                throw new IllegalArgumentException("Transfer quantity must be at least 1");
            }
            outgoing.merge(new UserCardId(transfer.fromUserId(), transfer.cardId()), transfer.quantity(), Integer::sum);
            incoming.merge(new UserCardId(transfer.toUserId(), transfer.cardId()), transfer.quantity(), Integer::sum);
        }

        Map<UserCardId, UserCard> lockedCards = userCardRepository.lockByUserIdsInOrder(userIds).stream()
                .collect(Collectors.toMap(card -> new UserCardId(card.getUserId(), card.getCardId()),
                        Function.identity()));
        outgoing.forEach((key, copies) -> {
            UserCard owned = lockedCards.get(key);
            if (owned == null || owned.getQuantity() < copies) {
                throw new UnprocessableException("INSUFFICIENT_CARDS",
                        "A user no longer owns enough copies to complete this trade", List.of());
            }
        });

        Set<UserCardId> changedCards = new TreeSet<>(Comparator.comparing(UserCardId::userId)
                .thenComparing(UserCardId::cardId));
        changedCards.addAll(outgoing.keySet());
        changedCards.addAll(incoming.keySet());
        for (UserCardId key : changedCards) {
            int removed = outgoing.getOrDefault(key, 0);
            int added = incoming.getOrDefault(key, 0);
            UserCard owned = lockedCards.get(key);
            if (owned == null) {
                userCardRepository.addCopiesAtomically(key.userId(), key.cardId(), added);
            } else if (added >= removed) {
                if (added > removed) {
                    owned.add(added - removed);
                }
            } else {
                owned.remove(removed - added);
                if (owned.getQuantity() == 0) {
                    userCardRepository.delete(owned);
                }
            }
        }
    }

    private List<CollectionEntry> ownedEntries(long userId) {
        Map<Long, Integer> quantities = ownedQuantities(userId);
        if (quantities.isEmpty()) {
            return List.of();
        }
        return catalogService.findCardsByIds(quantities.keySet()).stream()
                .map(card -> toEntry(card, quantities.get(card.id())))
                .toList();
    }

    private static Predicate<CollectionEntry> matches(CardFilter filter) {
        return entry -> matchesValue(filter.faction(), entry.faction())
                && matchesValue(filter.fighterClass(), entry.fighterClass())
                && matchesValue(filter.rarity(), entry.rarity());
    }

    private static boolean matchesValue(String wanted, String actual) {
        return wanted == null || wanted.isBlank() || Objects.equals(wanted, actual);
    }

    private static CollectionEntry toEntry(CatalogCard card, int quantity) {
        return new CollectionEntry(card.id(), card.code(), card.name(), card.faction(), card.fighterClass(),
                card.rarity(), card.atk(), card.def(), card.vel(), card.crt(), card.imageFile(), quantity);
    }
}
