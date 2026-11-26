package it.fightersprogram.cosmetics;

import java.util.List;

public record PlayerAppearance(SkinView avatar, List<CardVariantAppearance> cardVariants) {

    public PlayerAppearance {
        cardVariants = List.copyOf(cardVariants);
    }
}
