package it.fightersprogram.trading.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import it.fightersprogram.common.error.ConflictException;
import it.fightersprogram.common.error.ForbiddenException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long senderId;

    private Long recipientId;

    @Enumerated(EnumType.STRING)
    private TradeStatus status;

    private OffsetDateTime createdAt;

    private OffsetDateTime resolvedAt;

    @ElementCollection
    @CollectionTable(name = "trade_item", joinColumns = @JoinColumn(name = "trade_id"))
    private List<TradeItem> items = new ArrayList<>();

    public static Trade propose(long senderId, long recipientId, List<TradeItem> items) {
        Trade trade = new Trade();
        trade.senderId = senderId;
        trade.recipientId = recipientId;
        trade.status = TradeStatus.PENDING;
        trade.createdAt = OffsetDateTime.now();
        trade.items.addAll(items);
        return trade;
    }

    public void accept(long userId) {
        requireRecipient(userId, "Only the recipient can accept");
        resolve(TradeStatus.ACCEPTED);
    }

    public void decline(long userId) {
        requireRecipient(userId, "Only the recipient can decline");
        resolve(TradeStatus.DECLINED);
    }

    public void cancel(long userId) {
        if (senderId != userId) {
            throw new ForbiddenException("Only the sender can cancel");
        }
        resolve(TradeStatus.CANCELLED);
    }

    private void requireRecipient(long userId, String message) {
        if (recipientId != userId) {
            throw new ForbiddenException(message);
        }
    }

    private void resolve(TradeStatus resolution) {
        if (status != TradeStatus.PENDING) {
            throw new ConflictException("The trade is already " + status.name().toLowerCase());
        }
        status = resolution;
        resolvedAt = OffsetDateTime.now();
    }
}
