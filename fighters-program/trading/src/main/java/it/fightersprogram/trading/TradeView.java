package it.fightersprogram.trading;

import java.time.OffsetDateTime;
import java.util.List;

import it.fightersprogram.trading.domain.TradeStatus;

public record TradeView(long id, String senderNickname, String recipientNickname, TradeStatus status,
        List<TradeCardView> offered, List<TradeCardView> requested, OffsetDateTime createdAt,
        OffsetDateTime resolvedAt) {
}
