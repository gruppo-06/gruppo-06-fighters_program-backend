package it.fightersprogram.collection.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckSubcard {

    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "subcard_id")
    private Short subcardId;

    public DeckSubcard(long cardId, short subcardId) {
        this.cardId = cardId;
        this.subcardId = subcardId;
    }
}
