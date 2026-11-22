package it.fightersprogram.collection.domain;

import java.util.List;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCardRepository extends JpaRepository<UserCard, UserCardId> {

    List<UserCard> findByUserId(long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select card from UserCard card where card.userId in :userIds order by card.userId asc, card.cardId asc")
    List<UserCard> lockByUserIdsInOrder(@Param("userIds") List<Long> userIds);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO user_card (user_id, card_id, quantity) VALUES (:userId, :cardId, :copies)
            ON CONFLICT (user_id, card_id)
            DO UPDATE SET quantity = user_card.quantity + EXCLUDED.quantity
            """, nativeQuery = true)
    int addCopiesAtomically(@Param("userId") long userId, @Param("cardId") long cardId,
            @Param("copies") int copies);
}
