package it.fightersprogram.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    @EntityGraph(attributePaths = {"faction", "fighterClass", "rarity"})
    Optional<Card> findByCode(String code);

    @EntityGraph(attributePaths = {"faction", "fighterClass", "rarity"})
    List<Card> findAllByActiveTrueOrderByCodeAsc();

    @EntityGraph(attributePaths = {"faction", "fighterClass", "rarity"})
    List<Card> findAllByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = {"faction", "fighterClass", "rarity"})
    List<Card> findAllByCodeIn(Collection<String> codes);
}
