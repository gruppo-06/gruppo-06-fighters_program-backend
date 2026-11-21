package it.fightersprogram.economy;

import java.util.List;

public record CreditSummary(long balance, List<CreditMovement> movements) {
}
