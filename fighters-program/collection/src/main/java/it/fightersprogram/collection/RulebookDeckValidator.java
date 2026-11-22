package it.fightersprogram.collection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.CatalogCard;
import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.catalog.SubcardService;
import it.fightersprogram.catalog.SubcardView;
import it.fightersprogram.collection.domain.DeckLine;
import it.fightersprogram.collection.domain.DeckRulebook;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RulebookDeckValidator implements DeckValidator {

    private final DeckRules rules;
    private final CatalogService catalogService;
    private final SubcardService subcardService;
    private final CollectionService collectionService;

    @Override
    public List<Violation> validate(long userId, List<DeckEntry> entries) {
        List<Violation> violations = new ArrayList<>(duplicates(entries));
        Set<Long> cardIds = entries.stream().map(DeckEntry::cardId).collect(Collectors.toSet());
        Map<Long, CatalogCard> cards = catalogService.findCardsByIds(cardIds).stream()
                .collect(Collectors.toMap(CatalogCard::id, Function.identity()));
        entries.stream()
                .filter(entry -> !cards.containsKey(entry.cardId()))
                .forEach(entry -> violations.add(DeckRulebook.violation("UNKNOWN_CARD", entry.cardId())));
        if (!violations.isEmpty()) {
            return violations;
        }
        violations.addAll(unknownSubcards(entries));
        if (!violations.isEmpty()) {
            return violations;
        }
        Map<Long, Integer> owned = collectionService.ownedQuantities(userId);
        List<DeckLine> lines = entries.stream()
                .map(entry -> {
                    CatalogCard card = cards.get(entry.cardId());
                    return new DeckLine(card.id(), card.name(), card.rarity(), entry.copies(),
                            owned.getOrDefault(card.id(), 0), entry.subcardsOrEmpty());
                })
                .toList();
        return new DeckRulebook(rules).check(lines);
    }

    private List<Violation> unknownSubcards(List<DeckEntry> entries) {
        List<String> codes = entries.stream()
                .flatMap(entry -> entry.subcardsOrEmpty().stream())
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            return List.of();
        }
        Map<String, SubcardView> subcards = subcardService.findByCodes(codes);
        return codes.stream()
                .filter(code -> !subcards.containsKey(code))
                .map(code -> DeckRulebook.violation("UNKNOWN_SUBCARD", code))
                .toList();
    }

    private static List<Violation> duplicates(List<DeckEntry> entries) {
        Set<Long> seen = new HashSet<>();
        return entries.stream()
                .map(DeckEntry::cardId)
                .filter(cardId -> !seen.add(cardId))
                .distinct()
                .map(cardId -> DeckRulebook.violation("DUPLICATE_CARD", cardId))
                .toList();
    }
}
