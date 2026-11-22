package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CreditLedgerSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long userId;

    @BeforeEach
    void insertUser() {
        userId = jdbcTemplate.queryForObject("""
                INSERT INTO app_user (nickname, email, password_hash)
                VALUES ('ledger_user', 'ledger_user@example.com', 'hash')
                RETURNING id
                """, Long.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"WELCOME", "DAILY", "MATCH_WIN", "PACK_PURCHASE", "ADMIN_GRANT", "SKIN_PURCHASE",
            "TROPHY", "DAILY_CHALLENGE", "BATTLE_PASS"})
    void acceptsEveryTransactionType(String type) {
        assertDoesNotThrow(() -> insert(100, type, "key-" + type));
    }

    @Test
    void theBalanceIsTheSumOfTheMovements() {
        insert(500, "WELCOME", "welcome");
        insert(-120, "PACK_PURCHASE", "pack-1");
        insert(30, "DAILY", "daily-1");

        assertEquals(410, jdbcTemplate.queryForObject(
                "SELECT SUM(amount) FROM credit_transaction WHERE user_id = ?", Integer.class, userId));
    }

    @Test
    void rejectsAnUnknownType() {
        assertThrows(DataIntegrityViolationException.class, () -> insert(100, "LOTTERY", "lottery"));
    }

    @Test
    void rejectsAZeroAmount() {
        assertThrows(DataIntegrityViolationException.class, () -> insert(0, "ADMIN_GRANT", "zero"));
    }

    @Test
    void rejectsAReusedIdempotencyKey() {
        insert(100, "DAILY", "same-key");

        assertThrows(DataIntegrityViolationException.class, () -> insert(100, "DAILY", "same-key"));
    }

    @Test
    void rejectsAnUnknownUser() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO credit_transaction (user_id, amount, type, idempotency_key) VALUES (-1, 100, 'DAILY', 'k')
                """));
    }

    private void insert(int amount, String type, String idempotencyKey) {
        jdbcTemplate.update("""
                INSERT INTO credit_transaction (user_id, amount, type, reference, idempotency_key)
                VALUES (?, ?, ?, 'test', ?)
                """, userId, amount, type, idempotencyKey);
    }
}
