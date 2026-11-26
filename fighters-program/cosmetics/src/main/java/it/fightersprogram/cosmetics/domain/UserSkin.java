package it.fightersprogram.cosmetics.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_skin")
@IdClass(UserSkinId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSkin {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "skin_id", nullable = false)
    private Long skinId;

    @Column(name = "acquired_at", nullable = false)
    private OffsetDateTime acquiredAt;
}
