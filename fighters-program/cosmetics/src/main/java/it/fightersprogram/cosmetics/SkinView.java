package it.fightersprogram.cosmetics;

import it.fightersprogram.cosmetics.domain.SkinType;

public record SkinView(
        long id,
        String code,
        String name,
        SkinType type,
        String cardCode,
        String rarity,
        int price,
        String imageFile) {
}
