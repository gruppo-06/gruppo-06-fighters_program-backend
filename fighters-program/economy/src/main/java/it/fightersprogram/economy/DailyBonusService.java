package it.fightersprogram.economy;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.common.error.ConflictException;
import it.fightersprogram.common.error.NotFoundException;
import it.fightersprogram.economy.domain.CreditTransactionRepository;
import it.fightersprogram.economy.domain.DailyClaim;
import it.fightersprogram.economy.domain.DailyClaimRepository;

/**
 * One claim per game day (Europe/Rome). Claiming on the day after the last claim extends the streak, any other gap
 * restarts it at 1, and the reward stays at the last value once the streak is longer than the configured list.
 */
@Service
@Transactional(readOnly = true)
public class DailyBonusService {

    private final CreditService creditService;
    private final CreditTransactionRepository transactions;
    private final DailyClaimRepository claims;
    private final Clock clock;
    private final int[] rewards;

    public DailyBonusService(CreditService creditService, CreditTransactionRepository transactions,
            DailyClaimRepository claims, Clock clock, @Value("${game.credits.daily-streak}") int[] rewards) {
        this.creditService = creditService;
        this.transactions = transactions;
        this.claims = claims;
        this.clock = clock;
        this.rewards = rewards;
    }

    @Transactional
    public DailyBonusClaim claim(long userId) {
        transactions.lockUser(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
        LocalDate today = LocalDate.now(clock);
        DailyClaim previous = claims.findById(userId).orElse(null);
        if (previous != null && previous.getLastClaimDate().equals(today)) {
            throw new ConflictException("The daily bonus has already been claimed today");
        }
        int streak = activeStreak(previous, today) + 1;
        int amount = rewardFor(streak);
        creditService.credit(userId, amount, CreditType.DAILY, "daily:" + userId + ":" + today,
                "Daily bonus, day " + streak);
        if (previous == null) {
            claims.save(new DailyClaim(userId, today, streak));
        } else {
            previous.record(today, streak);
        }
        return new DailyBonusClaim(amount, streak, today.plusDays(1));
    }

    public DailyBonusStatus status(long userId) {
        LocalDate today = LocalDate.now(clock);
        DailyClaim previous = claims.findById(userId).orElse(null);
        int streak = activeStreak(previous, today);
        boolean claimableToday = previous == null || !previous.getLastClaimDate().equals(today);
        return new DailyBonusStatus(claimableToday, streak, rewardFor(streak + 1));
    }

    private static int activeStreak(DailyClaim claim, LocalDate today) {
        if (claim == null) {
            return 0;
        }
        LocalDate last = claim.getLastClaimDate();
        return last.equals(today) || last.equals(today.minusDays(1)) ? claim.getStreak() : 0;
    }

    private int rewardFor(int streak) {
        return rewards[Math.min(streak, rewards.length) - 1];
    }
}
