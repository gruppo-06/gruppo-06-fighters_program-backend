package it.fightersprogram.economy;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.CatalogCard;
import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.collection.CollectionService;
import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.common.error.UnprocessableException;
import it.fightersprogram.economy.domain.Pack;
import it.fightersprogram.economy.domain.PackContent;
import it.fightersprogram.economy.domain.PackOpening;
import it.fightersprogram.economy.domain.PackOpeningRepository;
import it.fightersprogram.economy.domain.PackRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Opens a pack in a single transaction: the price is debited, one card is drawn per content slot and every drawn
 * card is added to the collection. A repeated idempotency key replays the stored opening without charging again.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PackOpeningService {

    private static final TypeReference<List<Long>> CARD_IDS = new TypeReference<>() {
    };

    private final PackRepository packs;
    private final PackOpeningRepository openings;
    private final CatalogService catalogService;
    private final CollectionService collectionService;
    private final CreditService creditService;
    private final JsonMapper jsonMapper;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public PackOpeningResult open(long userId, long packId, String requestKey) {
        String openingKey = "pack-open:" + userId + ":" + requestKey;
        PackOpening existing = openings.findByIdempotencyKey(openingKey).orElse(null);
        if (existing != null) {
            return rebuild(existing);
        }

        Pack pack = packs.findByIdAndActiveTrue(packId)
                .orElseThrow(() -> new NotFoundException("Pack not found: " + packId));
        creditService.debit(userId, pack.getPrice(), CreditType.PACK_PURCHASE, openingKey, pack.getName());

        List<OpenedCard> drawn = draw(pack);
        drawn.forEach(card -> collectionService.grant(userId, card.cardId(), 1));
        String cards = jsonMapper.writeValueAsString(drawn.stream().map(OpenedCard::cardId).toList());
        PackOpening opening = openings.save(new PackOpening(userId, pack.getId(), cards, openingKey));
        return new PackOpeningResult(opening.getId(), pack.getId(), pack.getName(), pack.getPrice(), drawn,
                creditService.balance(userId));
    }

    private List<OpenedCard> draw(Pack pack) {
        Map<String, List<CatalogCard>> byRarity = catalogService.findAllCards().stream()
                .collect(Collectors.groupingBy(CatalogCard::rarity));
        List<OpenedCard> drawn = new ArrayList<>();
        for (PackContent content : pack.getContents()) {
            String rarity = packs.rarityName(content.getRarityId());
            List<CatalogCard> candidates = byRarity.getOrDefault(rarity, List.of());
            if (candidates.isEmpty()) {
                throw new UnprocessableException("PACK_UNAVAILABLE",
                        "The pack cannot be opened: no active " + rarity + " cards are available", List.of());
            }
            for (int card = 0; card < content.getCardCount(); card++) {
                CatalogCard picked = candidates.get(random.nextInt(candidates.size()));
                drawn.add(new OpenedCard(picked.id(), picked.code(), picked.name(), picked.rarity()));
            }
        }
        return drawn;
    }

    private PackOpeningResult rebuild(PackOpening opening) {
        Pack pack = packs.findById(opening.getPackId())
                .orElseThrow(() -> new NotFoundException("Pack not found: " + opening.getPackId()));
        List<Long> cardIds = jsonMapper.readValue(opening.getCards(), CARD_IDS);
        Map<Long, CatalogCard> byId = catalogService.findCardsByIds(cardIds).stream()
                .collect(Collectors.toMap(CatalogCard::id, Function.identity()));
        List<OpenedCard> cards = cardIds.stream()
                .map(id -> {
                    CatalogCard card = byId.get(id);
                    return new OpenedCard(card.id(), card.code(), card.name(), card.rarity());
                })
                .toList();
        return new PackOpeningResult(opening.getId(), pack.getId(), pack.getName(), pack.getPrice(), cards,
                creditService.balance(opening.getUserId()));
    }
}
