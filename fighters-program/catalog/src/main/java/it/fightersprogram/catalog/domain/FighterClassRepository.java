package it.fightersprogram.catalog.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FighterClassRepository extends JpaRepository<FighterClass, Short> {

    Optional<FighterClass> findByName(String name);
}
