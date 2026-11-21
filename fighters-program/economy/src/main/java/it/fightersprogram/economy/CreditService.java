package it.fightersprogram.economy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.economy.domain.CreditTransaction;
import it.fightersprogram.economy.domain.CreditTransactionRepository;

import lombok.RequiredArgsConstructor;

/**
 * The balance is the sum of the ledger movements. Writes lock the user's {@code app_user} row, so concurrent
 * debits are serialised, and a repeated idempotency key returns the first movement instead of applying it again.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditService {

    private final CreditTransactionRepository transactions;

    public long balance(long userId) {
        return transactions.balanceOf(userId);
    }

    @Transactional
    public CreditMovement credit(long userId, int amount, CreditType type, String idempotencyKey) {
        return credit(userId, amount, type, idempotencyKey, null);
    }

    @Transactional
    public CreditMovement credit(long userId, int amount, CreditType type, String idempotencyKey, String reference) {
        return record(userId, requirePositive(amount), type, idempotencyKey, reference);
    }

    @Transactional
    public CreditMovement debit(long userId, int amount, CreditType type, String idempotencyKey) {
        return debit(userId, amount, type, idempotencyKey, null);
    }

    @Transactional
    public CreditMovement debit(long userId, int amount, CreditType type, String idempotencyKey, String reference) {
        return record(userId, -requirePositive(amount), type, idempotencyKey, reference);
    }

    private CreditMovement record(long userId, int signedAmount, CreditType type, String idempotencyKey,
            String reference) {
        transactions.lockUser(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return transactions.findByIdempotencyKey(idempotencyKey)
                .map(CreditService::toMovement)
                .orElseGet(() -> {
                    long balance = transactions.balanceOf(userId);
                    if (balance + signedAmount < 0) {
                        throw new InsufficientCreditsException(balance, -signedAmount);
                    }
                    return toMovement(transactions.save(
                            new CreditTransaction(userId, signedAmount, type, reference, idempotencyKey)));
                });
    }

    private static int requirePositive(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("Credit amounts must be positive, got " + amount);
        }
        return amount;
    }

    static CreditMovement toMovement(CreditTransaction transaction) {
        return new CreditMovement(transaction.getId(), transaction.getAmount(), transaction.getType(),
                transaction.getReference(), transaction.getCreatedAt());
    }
}
