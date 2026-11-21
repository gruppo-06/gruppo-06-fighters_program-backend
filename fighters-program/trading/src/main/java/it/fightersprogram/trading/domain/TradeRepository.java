package it.fightersprogram.trading.domain;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByRecipientIdAndStatusOrderByCreatedAtDesc(long recipientId, TradeStatus status);

    List<Trade> findBySenderIdOrderByCreatedAtDesc(long senderId);

    List<Trade> findBySenderIdAndStatus(long senderId, TradeStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select trade from Trade trade where trade.id = :tradeId")
    Optional<Trade> lockById(@Param("tradeId") long tradeId);
}
