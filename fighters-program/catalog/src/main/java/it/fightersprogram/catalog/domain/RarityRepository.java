package it.fightersprogram.catalog.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RarityRepository extends JpaRepository<Rarity, Short> {

    Optional<Rarity> findByName(String name);
}
