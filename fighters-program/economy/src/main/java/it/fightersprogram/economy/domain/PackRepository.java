package it.fightersprogram.economy.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PackRepository extends JpaRepository<Pack, Long> {

    @Query("SELECT p FROM Pack p WHERE p.active = true ORDER BY p.price ASC")
    List<Pack> findAllByActiveTrueOrderByPriceAsc();

    Optional<Pack> findByIdAndActiveTrue(Long id);
}