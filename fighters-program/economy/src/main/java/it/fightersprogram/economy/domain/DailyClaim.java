package it.fightersprogram.economy.domain;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "daily_claim")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyClaim {

    @Id
    private Long userId;

    private LocalDate lastClaimDate;

    private int streak;

    public DailyClaim(long userId, LocalDate claimDate, int streak) {
        this.userId = userId;
        record(claimDate, streak);
    }

    public void record(LocalDate claimDate, int streak) {
        if (streak < 1) {
            throw new IllegalArgumentException("A streak starts at 1, got " + streak);
        }
        this.lastClaimDate = claimDate;
        this.streak = streak;
    }
}
