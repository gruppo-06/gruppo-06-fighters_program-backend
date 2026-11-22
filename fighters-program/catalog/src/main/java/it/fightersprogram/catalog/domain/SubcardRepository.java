package it.fightersprogram.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubcardRepository extends JpaRepository<Subcard, Short> {

    Optional<Subcard> findByCode(String code);

    List<Subcard> findAllByOrderByCodeAsc();

    List<Subcard> findAllByCodeIn(Collection<String> codes);
}
