package it.fightersprogram.cosmetics.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSkinRepository extends JpaRepository<UserSkin, UserSkinId> {

    List<UserSkin> findAllByUserIdOrderByAcquiredAtDescSkinIdAsc(long userId);

    @Modifying
    @Query(value = """
            INSERT INTO user_skin (user_id, skin_id, acquired_at) VALUES (:userId, :skinId, now())
            ON CONFLICT (user_id, skin_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") long userId, @Param("skinId") long skinId);
}
