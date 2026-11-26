package it.fightersprogram.cosmetics.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SkinRepository extends JpaRepository<Skin, Long>, JpaSpecificationExecutor<Skin> {

    Optional<Skin> findByCode(String code);
}
