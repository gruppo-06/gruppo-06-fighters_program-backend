package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.economy.CreditMovement;
import it.fightersprogram.economy.CreditService;
import it.fightersprogram.economy.CreditType;
import it.fightersprogram.economy.InsufficientCreditsException;

@SpringBootTest
@Transactional
class CreditServiceTest {

    @Autowired
    private CreditService creditService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long userId;

    @BeforeEach
    void insertUser() {
        userId = jdbcTemplate.queryForObject("""
                INSERT INTO app_user (nickname, email, password_hash)
                VALUES ('credit_user', 'credit_user@example.com', 'hash')
                RETURNING id
                """, Long.class);
    }

    @Test
    void aUserWithoutMovementsHasAZeroBalance() {
        assertEquals(0, creditService.balance(userId));
    }

    @Test
    void creditsAndDebitsAreRecordedAsMovements() {
        CreditMovement credit = creditService.credit(userId, 500, CreditType.WELCOME, "welcome:" + userId);
        CreditMovement debit = creditService.debit(userId, 120, CreditType.PACK_PURCHASE, "pack:1");

        assertEquals(500, credit.amount());
        assertEquals(CreditType.WELCOME, credit.type());
        assertEquals(-120, debit.amount());
        assertEquals(380, creditService.balance(userId));
        assertEquals(2, movementCount());
    }

    @Test
    void aReferenceIsStoredWithTheMovement() {
        CreditMovement movement = creditService.credit(userId, 50, CreditType.ADMIN_GRANT, "admin-grant:1",
                "refund by admin");

        assertEquals("refund by admin", movement.reference());
    }

    @Test
    void theSameIdempotencyKeyIsAppliedOnlyOnce() {
        CreditMovement first = creditService.credit(userId, 30, CreditType.DAILY, "daily:2026-11-21");
        CreditMovement second = creditService.credit(userId, 30, CreditType.DAILY, "daily:2026-11-21");

        assertEquals(first.id(), second.id());
        assertEquals(30, creditService.balance(userId));
        assertEquals(1, movementCount());
    }

    @Test
    void aDebitLargerThanTheBalanceIsRejectedWithoutAMovement() {
        creditService.credit(userId, 100, CreditType.WELCOME, "welcome");

        assertThrows(InsufficientCreditsException.class,
                () -> creditService.debit(userId, 101, CreditType.PACK_PURCHASE, "pack:too-expensive"));

        assertEquals(100, creditService.balance(userId));
        assertEquals(1, movementCount());
    }

    @Test
    void amountsMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> creditService.credit(userId, 0, CreditType.DAILY, "zero"));
        assertThrows(IllegalArgumentException.class,
                () -> creditService.debit(userId, -5, CreditType.PACK_PURCHASE, "negative"));
    }

    @Test
    void anUnknownUserIsNotFound() {
        assertThrows(NotFoundException.class, () -> creditService.credit(-1, 10, CreditType.DAILY, "ghost"));
    }

    private int movementCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM credit_transaction WHERE user_id = ?",
                Integer.class, userId);
    }
}
