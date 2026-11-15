package it.fightersprogram.catalog.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FactionRepository extends JpaRepository<Faction, Short> {

    Optional<Faction> findByName(String name);
}
