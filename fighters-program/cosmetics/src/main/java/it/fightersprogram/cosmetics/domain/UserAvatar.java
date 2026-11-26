package it.fightersprogram.cosmetics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_avatar")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAvatar {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "skin_id", nullable = false)
    private Long skinId;
}
