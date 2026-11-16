package it.fightersprogram.collection.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckCard {

    private Long cardId;

    private int copies;

    public DeckCard(long cardId, int copies) {
        this.cardId = cardId;
        this.copies = copies;
    }
}
