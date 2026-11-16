package it.fightersprogram.collection;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("game.deck")
public record DeckRules(int size, int maxCopies, int maxEpic, int maxLegendary, int powerBudget, int maxPerUser,
        Map<String, Integer> rarityPoints) {

    public int pointsOf(String rarity) {
        Integer points = rarityPoints.get(rarity);
        if (points == null) {
            throw new IllegalStateException("No game.deck.rarity-points value for rarity " + rarity);
        }
        return points;
    }
}
