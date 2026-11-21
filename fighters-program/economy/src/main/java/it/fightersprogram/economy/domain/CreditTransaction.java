package it.fightersprogram.economy.domain;

import java.time.OffsetDateTime;

import it.fightersprogram.economy.CreditType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "credit_transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private int amount;

    @Enumerated(EnumType.STRING)
    private CreditType type;

    private String reference;

    private String idempotencyKey;

    private OffsetDateTime createdAt;

    public CreditTransaction(long userId, int amount, CreditType type, String reference, String idempotencyKey) {
        if (amount == 0) {
            throw new IllegalArgumentException("A credit movement cannot be zero");
        }
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.reference = reference;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = OffsetDateTime.now();
    }
}
