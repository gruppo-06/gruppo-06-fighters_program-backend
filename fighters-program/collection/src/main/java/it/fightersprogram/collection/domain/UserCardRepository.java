package it.fightersprogram.collection.domain;

import java.util.List;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCardRepository extends JpaRepository<UserCard, UserCardId> {

    List<UserCard> findByUserId(long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select card from UserCard card where card.userId in :userIds order by card.userId asc, card.cardId asc")
    List<UserCard> lockByUserIdsInOrder(@Param("userIds") List<Long> userIds);
}
