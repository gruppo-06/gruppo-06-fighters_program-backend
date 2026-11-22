package it.fightersprogram.economy.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {

    @Query("select coalesce(sum(movement.amount), 0) from CreditTransaction movement where movement.userId = :userId")
    long balanceOf(@Param("userId") long userId);

    Optional<CreditTransaction> findByIdempotencyKey(String idempotencyKey);

    List<CreditTransaction> findTop20ByUserIdOrderByCreatedAtDescIdDesc(long userId);

    @Query(value = "SELECT id FROM app_user WHERE id = :userId FOR UPDATE", nativeQuery = true)
    Optional<Long> lockUser(@Param("userId") long userId);
}
