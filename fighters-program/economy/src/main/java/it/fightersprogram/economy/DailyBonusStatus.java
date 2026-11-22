package it.fightersprogram.economy;

public record DailyBonusStatus(boolean claimableToday, int streak, int nextAmount) {
}
