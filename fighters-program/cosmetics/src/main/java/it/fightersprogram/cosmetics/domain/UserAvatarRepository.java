package it.fightersprogram.cosmetics.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAvatarRepository extends JpaRepository<UserAvatar, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO user_avatar (user_id, skin_id) VALUES (:userId, :skinId)
            ON CONFLICT (user_id) DO UPDATE SET skin_id = EXCLUDED.skin_id
            """, nativeQuery = true)
    int upsert(@Param("userId") long userId, @Param("skinId") long skinId);
}
