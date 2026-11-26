package it.fightersprogram.cosmetics.domain;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode
public class UserSkinId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long skinId;

    public UserSkinId(long userId, long skinId) {
        this.userId = userId;
        this.skinId = skinId;
    }
}
