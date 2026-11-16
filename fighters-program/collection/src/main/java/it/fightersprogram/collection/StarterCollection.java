package it.fightersprogram.collection;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.identity.UserRegistered;
import lombok.RequiredArgsConstructor;

/**
 * Provisional starter cards (decision 003) so a new player can build a deck before packs exist (SWD_EC02).
 */
@Component
@ConditionalOnProperty(name = "game.starter-collection.enabled", havingValue = "true")
@RequiredArgsConstructor
public class StarterCollection {

    private static final Map<String, Integer> COPIES_BY_RARITY = Map.of("Comune", 2, "Rara", 1);

    private final CatalogService catalogService;
    private final CollectionService collectionService;

    @EventListener
    public void grantStarterCards(UserRegistered event) {
        catalogService.findAllCards().stream()
                .filter(card -> COPIES_BY_RARITY.containsKey(card.rarity()))
                .forEach(card -> collectionService.grant(event.userId(), card.id(),
                        COPIES_BY_RARITY.get(card.rarity())));
    }
}
