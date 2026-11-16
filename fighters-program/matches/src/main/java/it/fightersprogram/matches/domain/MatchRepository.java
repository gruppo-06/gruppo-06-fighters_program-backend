package it.fightersprogram.matches.domain;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByOpponentIdAndStatusOrderByCreatedAtDesc(long opponentId, MatchStatus status);

    List<Match> findByChallengerIdOrderByCreatedAtDesc(long challengerId);

    boolean existsByChallengerDeckIdAndStatus(long challengerDeckId, MatchStatus status);

    @Query("""
            SELECT m FROM Match m
            WHERE m.status = it.fightersprogram.matches.domain.MatchStatus.FINISHED
              AND (m.challengerId = :userId OR m.opponentId = :userId)
            ORDER BY m.finishedAt DESC, m.id DESC
            """)
    Page<Match> findFinishedFor(@Param("userId") long userId, Pageable pageable);
}
