package it.fightersprogram.collection.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    List<Deck> findByUserIdOrderByCreatedAtAscIdAsc(long userId);

    List<Deck> findByUserIdOrderByCreatedAtDescIdDesc(long userId);

    @Query(value = """
            SELECT DISTINCT d.user_id FROM deck d
            JOIN app_user u ON u.id = d.user_id
            WHERE u.role = 'PLAYER' AND d.user_id <> :userId
            """, nativeQuery = true)
    List<Long> findPlayerIdsWithDecksExcept(long userId);

    Optional<Deck> findByIdAndUserId(long id, long userId);

    boolean existsByUserIdAndName(long userId, String name);

    boolean existsByUserIdAndNameAndIdNot(long userId, String name, long id);

    long countByUserId(long userId);
}
