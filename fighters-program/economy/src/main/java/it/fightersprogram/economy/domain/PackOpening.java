package it.fightersprogram.economy.domain;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pack_opening")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PackOpening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long packId;

    @JdbcTypeCode(SqlTypes.JSON)
    private String cards;

    private String idempotencyKey;

    private OffsetDateTime createdAt;

    public PackOpening(long userId, long packId, String cards, String idempotencyKey) {
        this.userId = userId;
        this.packId = packId;
        this.cards = cards;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = OffsetDateTime.now();
    }
}