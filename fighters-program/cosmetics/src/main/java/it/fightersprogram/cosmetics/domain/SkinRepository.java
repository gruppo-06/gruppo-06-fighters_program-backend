package it.fightersprogram.cosmetics.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SkinRepository extends JpaRepository<Skin, Long> {

    Optional<Skin> findByCode(String code);
}
