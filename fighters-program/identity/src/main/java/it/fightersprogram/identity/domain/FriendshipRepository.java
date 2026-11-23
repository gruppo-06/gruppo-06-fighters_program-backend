package it.fightersprogram.identity.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.fightersprogram.identity.domain.Friendship.Status;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("""
            select friendship from Friendship friendship
            where (friendship.requesterId = :first and friendship.addresseeId = :second)
               or (friendship.requesterId = :second and friendship.addresseeId = :first)
            """)
    Optional<Friendship> findBetween(@Param("first") long first, @Param("second") long second);

    Optional<Friendship> findByIdAndAddresseeId(long id, long addresseeId);

    List<Friendship> findByAddresseeIdAndStatusOrderByCreatedAtDesc(long addresseeId, Status status);

    List<Friendship> findByRequesterIdAndStatusOrderByCreatedAtDesc(long requesterId, Status status);

    @Query("""
            select friendship from Friendship friendship
            where friendship.status = :status
              and (friendship.requesterId = :userId or friendship.addresseeId = :userId)
            """)
    List<Friendship> findByStatusInvolving(@Param("userId") long userId, @Param("status") Status status);
}
