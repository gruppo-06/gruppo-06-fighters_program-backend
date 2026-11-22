package it.fightersprogram.economy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PackContent {

    @Column(name = "rarity_id")
    private Short rarityId;

    @Column(name = "card_count")
    private int cardCount;

    public PackContent(short rarityId, int cardCount) {
        if (cardCount < 1) {
            throw new IllegalArgumentException("cardCount must be >= 1");
        }
        this.rarityId = rarityId;
        this.cardCount = cardCount;
    }
}