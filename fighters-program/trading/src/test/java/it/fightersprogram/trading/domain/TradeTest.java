package it.fightersprogram.trading.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fightersprogram.common.error.ConflictException;
import it.fightersprogram.common.error.ForbiddenException;

class TradeTest {

    private static final long SENDER = 1;
    private static final long RECIPIENT = 2;
    private static final long OUTSIDER = 3;

    @Test
    void proposalStartsPendingWithBothSidesAndCreationTime() {
        Trade trade = trade();

        assertEquals(TradeStatus.PENDING, trade.getStatus());
        assertNotNull(trade.getCreatedAt());
        assertEquals(2, trade.getItems().size());
        assertEquals(TradeSide.OFFERED, trade.getItems().get(0).getSide());
        assertEquals(TradeSide.REQUESTED, trade.getItems().get(1).getSide());
    }

    @Test
    void recipientCanAccept() {
        Trade trade = trade();

        trade.accept(RECIPIENT);

        assertEquals(TradeStatus.ACCEPTED, trade.getStatus());
        assertNotNull(trade.getResolvedAt());
    }

    @Test
    void onlyRecipientCanAcceptOrDecline() {
        assertThrows(ForbiddenException.class, () -> trade().accept(SENDER));
        assertThrows(ForbiddenException.class, () -> trade().decline(OUTSIDER));
    }

    @Test
    void recipientCanDecline() {
        Trade trade = trade();

        trade.decline(RECIPIENT);

        assertEquals(TradeStatus.DECLINED, trade.getStatus());
        assertNotNull(trade.getResolvedAt());
    }

    @Test
    void onlySenderCanCancel() {
        Trade trade = trade();

        assertThrows(ForbiddenException.class, () -> trade.cancel(RECIPIENT));
        trade.cancel(SENDER);

        assertEquals(TradeStatus.CANCELLED, trade.getStatus());
        assertNotNull(trade.getResolvedAt());
    }

    @Test
    void closedTradeCannotBeResolvedAgain() {
        Trade accepted = trade();
        accepted.accept(RECIPIENT);
        Trade declined = trade();
        declined.decline(RECIPIENT);
        Trade cancelled = trade();
        cancelled.cancel(SENDER);

        assertThrows(ConflictException.class, () -> accepted.accept(RECIPIENT));
        assertThrows(ConflictException.class, () -> declined.cancel(SENDER));
        assertThrows(ConflictException.class, () -> cancelled.decline(RECIPIENT));
    }

    private Trade trade() {
        return Trade.propose(SENDER, RECIPIENT, List.of(
                new TradeItem(10, 3, TradeSide.OFFERED),
                new TradeItem(20, 1, TradeSide.REQUESTED)));
    }
}
