package it.fightersprogram.economy.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PackOpeningRepository extends JpaRepository<PackOpening, Long> {

    Optional<PackOpening> findByIdempotencyKey(String idempotencyKey);
}