package it.fightersprogram.trading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.CatalogCard;
import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.collection.CollectionService;
import it.fightersprogram.common.error.BadRequestException;
import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.common.error.UnprocessableException;
import it.fightersprogram.identity.UserDirectory;
import it.fightersprogram.trading.domain.Trade;
import it.fightersprogram.trading.domain.TradeItem;
import it.fightersprogram.trading.domain.TradeRepository;
import it.fightersprogram.trading.domain.TradeSide;
import it.fightersprogram.trading.domain.TradeStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeService {

    private final TradeRepository tradeRepository;
    private final UserDirectory userDirectory;
    private final CatalogService catalogService;
    private final CollectionService collectionService;

    @Transactional
    public TradeView propose(long senderId, CreateTradeRequest request) {
        long recipientId = userDirectory.findIdByNickname(request.recipientNickname())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.recipientNickname()));
        if (senderId == recipientId) {
            throw new BadRequestException("You cannot trade with yourself");
        }

        List<Long> cardIds = new ArrayList<>();
        request.offered().forEach(card -> cardIds.add(card.cardId()));
        request.requested().forEach(card -> cardIds.add(card.cardId()));
        Map<Long, CatalogCard> catalog = catalogService.findCardsByIds(cardIds).stream()
                .collect(Collectors.toMap(CatalogCard::id, Function.identity()));
        if (catalog.size() != cardIds.stream().distinct().count()) {
            throw invalid("One or more cards do not exist in the catalog");
        }

        Map<Long, Integer> senderOwned = collectionService.ownedQuantities(senderId);
        Map<Long, Integer> committed = pendingOfferedQuantities(senderId);
        requireAvailable(request.offered(), senderOwned, committed, "You do not have enough uncommitted copies");
        requireAvailable(request.requested(), collectionService.ownedQuantities(recipientId), Map.of(),
                "The recipient does not own enough copies");

        List<TradeItem> items = new ArrayList<>();
        request.offered().forEach(card -> items.add(new TradeItem(card.cardId(), card.quantity(), TradeSide.OFFERED)));
        request.requested().forEach(card -> items.add(new TradeItem(card.cardId(), card.quantity(), TradeSide.REQUESTED)));
        Trade trade = tradeRepository.save(Trade.propose(senderId, recipientId, items));
        return toView(trade, catalog);
    }

    public List<TradeView> incoming(long userId) {
        return tradeRepository.findByRecipientIdAndStatusOrderByCreatedAtDesc(userId, TradeStatus.PENDING).stream()
                .map(this::toView)
                .toList();
    }

    public List<TradeView> outgoing(long userId) {
        return tradeRepository.findBySenderIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toView)
                .toList();
    }

    public TradeView findVisibleTrade(long userId, long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .filter(candidate -> candidate.getSenderId() == userId || candidate.getRecipientId() == userId)
                .orElseThrow(() -> new NotFoundException("Trade not found: " + tradeId));
        return toView(trade);
    }

    private Map<Long, Integer> pendingOfferedQuantities(long senderId) {
        Map<Long, Integer> quantities = new HashMap<>();
        tradeRepository.findBySenderIdAndStatus(senderId, TradeStatus.PENDING).forEach(trade -> trade.getItems().stream()
                .filter(item -> item.getSide() == TradeSide.OFFERED)
                .forEach(item -> quantities.merge(item.getCardId(), item.getQuantity(), Integer::sum)));
        return quantities;
    }

    private static void requireAvailable(List<TradeCardRequest> requested, Map<Long, Integer> owned,
            Map<Long, Integer> committed, String message) {
        if (requested.stream().anyMatch(card -> owned.getOrDefault(card.cardId(), 0)
                - committed.getOrDefault(card.cardId(), 0) < card.quantity())) {
            throw invalid(message);
        }
    }

    private TradeView toView(Trade trade, Map<Long, CatalogCard> catalog) {
        List<TradeCardView> offered = trade.getItems().stream().filter(item -> item.getSide() == TradeSide.OFFERED)
                .map(item -> view(item, catalog)).toList();
        List<TradeCardView> requested = trade.getItems().stream().filter(item -> item.getSide() == TradeSide.REQUESTED)
                .map(item -> view(item, catalog)).toList();
        return new TradeView(trade.getId(), userDirectory.nicknameOf(trade.getSenderId()),
                userDirectory.nicknameOf(trade.getRecipientId()), trade.getStatus(), offered, requested,
                trade.getCreatedAt(), trade.getResolvedAt());
    }

    private TradeView toView(Trade trade) {
        List<Long> cardIds = trade.getItems().stream().map(TradeItem::getCardId).distinct().toList();
        Map<Long, CatalogCard> catalog = catalogService.findCardsByIds(cardIds).stream()
                .collect(Collectors.toMap(CatalogCard::id, Function.identity()));
        return toView(trade, catalog);
    }

    private static TradeCardView view(TradeItem item, Map<Long, CatalogCard> catalog) {
        CatalogCard card = catalog.get(item.getCardId());
        return new TradeCardView(card.id(), card.code(), card.name(), item.getQuantity());
    }

    private static UnprocessableException invalid(String message) {
        return new UnprocessableException("INVALID_TRADE", message, List.of());
    }
}
