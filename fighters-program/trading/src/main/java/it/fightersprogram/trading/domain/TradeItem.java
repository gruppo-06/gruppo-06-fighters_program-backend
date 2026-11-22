package it.fightersprogram.trading.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeItem {

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeSide side;

    public TradeItem(long cardId, int quantity, TradeSide side) {
        this.cardId = cardId;
        this.quantity = quantity;
        this.side = side;
    }
}
