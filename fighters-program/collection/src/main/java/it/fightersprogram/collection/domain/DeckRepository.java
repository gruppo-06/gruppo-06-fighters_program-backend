package it.fightersprogram.collection.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    List<Deck> findByUserIdOrderByCreatedAtAscIdAsc(long userId);

    Optional<Deck> findByIdAndUserId(long id, long userId);

    boolean existsByUserIdAndName(long userId, String name);

    boolean existsByUserIdAndNameAndIdNot(long userId, String name, long id);

    long countByUserId(long userId);
}
