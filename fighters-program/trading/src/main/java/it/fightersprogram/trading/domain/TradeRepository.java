package it.fightersprogram.trading.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByRecipientIdAndStatusOrderByCreatedAtDesc(long recipientId, TradeStatus status);

    List<Trade> findBySenderIdOrderByCreatedAtDesc(long senderId);
}
