package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import it.fightersprogram.trading.domain.Trade;
import it.fightersprogram.trading.domain.TradeItem;
import it.fightersprogram.trading.domain.TradeRepository;
import it.fightersprogram.trading.domain.TradeSide;

@SpringBootTest
@Transactional
class MatchAndTradeSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private EntityManager entityManager;

    private long challengerId;
    private long opponentId;
    private long outsiderId;
    private long challengerDeckId;
    private long cardId;

    @BeforeEach
    void insertPlayersDeckAndCard() {
        challengerId = insertUser("challenger");
        opponentId = insertUser("opponent");
        outsiderId = insertUser("outsider");
        challengerDeckId = jdbcTemplate.queryForObject(
                "INSERT INTO deck (user_id, name) VALUES (?, 'Main') RETURNING id", Long.class, challengerId);
        cardId = jdbcTemplate.queryForObject("""
                INSERT INTO card (code, name, faction_id, class_id, rarity_id, atk, def, vel, crt)
                VALUES ('ZZ98', 'Schema Test Card', 1, 1, 1, 100, 100, 100, 100)
                RETURNING id
                """, Long.class);
    }

    @Test
    void pendingMatchNeedsNoOpponentDeckWinnerSeedOrLog() {
        assertDoesNotThrow(() -> insertMatch(challengerId, opponentId, "PENDING"));
    }

    @Test
    void matchStatusMustBeKnown() {
        assertThrows(DataIntegrityViolationException.class, () -> insertMatch(challengerId, opponentId, "PLAYING"));
    }

    @Test
    void playerCannotChallengeThemselves() {
        assertThrows(DataIntegrityViolationException.class, () -> insertMatch(challengerId, challengerId, "PENDING"));
    }

    @Test
    void winnerMustBeOneOfThePlayers() {
        long matchId = insertMatch(challengerId, opponentId, "PENDING");

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "UPDATE match SET status = 'FINISHED', finished_at = now(), winner_id = ? WHERE id = ?",
                outsiderId, matchId));
    }

    @Test
    void finishedMatchMustHaveFinishTime() {
        long matchId = insertMatch(challengerId, opponentId, "PENDING");

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "UPDATE match SET status = 'FINISHED', winner_id = ? WHERE id = ?", challengerId, matchId));
    }

    @Test
    void matchLogIsStoredAsJson() {
        long matchId = insertMatch(challengerId, opponentId, "PENDING");

        jdbcTemplate.update("UPDATE match SET log = CAST(? AS jsonb) WHERE id = ?", "{\"turns\": []}", matchId);

        String logType = jdbcTemplate.queryForObject(
                "SELECT jsonb_typeof(log) FROM match WHERE id = ?", String.class, matchId);
        assertEquals("object", logType);
    }

    @Test
    void playerCannotTradeWithThemselves() {
        assertThrows(DataIntegrityViolationException.class, () -> insertTrade(challengerId, challengerId));
    }

    @Test
    void tradeItemSideMustBeKnown() {
        long tradeId = insertTrade(challengerId, opponentId);

        assertThrows(DataIntegrityViolationException.class, () -> insertTradeItem(tradeId, 1, "GIVEN"));
    }

    @Test
    void tradeItemQuantityMustBeAtLeastOne() {
        long tradeId = insertTrade(challengerId, opponentId);

        assertThrows(DataIntegrityViolationException.class, () -> insertTradeItem(tradeId, 0, "OFFERED"));
    }

    @Test
    void sameCardCanBeBothOfferedAndRequested() {
        long tradeId = insertTrade(challengerId, opponentId);
        insertTradeItem(tradeId, 1, "OFFERED");

        assertDoesNotThrow(() -> insertTradeItem(tradeId, 1, "REQUESTED"));
    }

    @Test
    void deletingTradeDeletesItsItems() {
        long tradeId = insertTrade(challengerId, opponentId);
        insertTradeItem(tradeId, 1, "OFFERED");

        jdbcTemplate.update("DELETE FROM trade WHERE id = ?", tradeId);

        Integer remainingItems = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM trade_item WHERE trade_id = ?", Integer.class, tradeId);
        assertEquals(0, remainingItems);
    }

    @Test
    void tradeAndItemsCanBePersistedAndReadBack() {
        Trade trade = Trade.propose(challengerId, opponentId, List.of(
                new TradeItem(cardId, 3, TradeSide.OFFERED),
                new TradeItem(cardId, 1, TradeSide.REQUESTED)));

        Trade saved = tradeRepository.saveAndFlush(trade);
        Long tradeId = saved.getId();
        entityManager.clear();

        Trade loaded = tradeRepository.findById(tradeId).orElseThrow();
        assertEquals(2, loaded.getItems().size());
        assertEquals(3, loaded.getItems().stream()
                .filter(item -> item.getSide() == TradeSide.OFFERED).findFirst().orElseThrow().getQuantity());
        assertEquals(1, loaded.getItems().stream()
                .filter(item -> item.getSide() == TradeSide.REQUESTED).findFirst().orElseThrow().getQuantity());
    }

    private long insertUser(String nickname) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO app_user (nickname, email, password_hash)
                VALUES (?, ?, 'hash')
                RETURNING id
                """, Long.class, "schema_" + nickname, nickname + "@example.com");
    }

    private long insertMatch(long challenger, long opponent, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO match (challenger_id, opponent_id, challenger_deck_id, status)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """, Long.class, challenger, opponent, challengerDeckId, status);
    }

    private long insertTrade(long sender, long recipient) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO trade (sender_id, recipient_id) VALUES (?, ?) RETURNING id", Long.class, sender, recipient);
    }

    private void insertTradeItem(long tradeId, int quantity, String side) {
        jdbcTemplate.update("INSERT INTO trade_item (trade_id, card_id, quantity, side) VALUES (?, ?, ?, ?)",
                tradeId, cardId, quantity, side);
    }
}
