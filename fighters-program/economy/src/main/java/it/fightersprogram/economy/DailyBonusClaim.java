package it.fightersprogram.economy;

import java.time.LocalDate;

public record DailyBonusClaim(int amount, int streak, LocalDate nextClaimDate) {
}
