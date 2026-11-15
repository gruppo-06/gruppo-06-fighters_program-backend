package it.fightersprogram.engine;

public record CardSummary(
        PlayerSide player,
        int deckPosition,
        String cardCode,
        int enteredOnTurn,
        int roundsPlayed,
        int damageDealt,
        int damageTaken,
        int knockouts) {
}
