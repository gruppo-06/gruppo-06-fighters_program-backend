package it.fightersprogram.cosmetics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_card_variant")
@IdClass(UserCardVariantId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCardVariant {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "skin_id", nullable = false)
    private Long skinId;
}
