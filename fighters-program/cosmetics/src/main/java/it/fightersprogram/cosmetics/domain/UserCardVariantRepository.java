package it.fightersprogram.cosmetics.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCardVariantRepository extends JpaRepository<UserCardVariant, UserCardVariantId> {

    List<UserCardVariant> findAllByUserIdOrderByCardIdAsc(long userId);

    @Modifying
    @Query(value = """
            INSERT INTO user_card_variant (user_id, card_id, skin_id) VALUES (:userId, :cardId, :skinId)
            ON CONFLICT (user_id, card_id) DO UPDATE SET skin_id = EXCLUDED.skin_id
            """, nativeQuery = true)
    int upsert(@Param("userId") long userId, @Param("cardId") long cardId, @Param("skinId") long skinId);
}
