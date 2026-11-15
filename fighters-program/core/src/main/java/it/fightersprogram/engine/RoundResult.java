package it.fightersprogram.engine;

public record RoundResult(FighterRound first, FighterRound second) {

    public RoundOutcome outcome() {
        if (first.isKnockedOut() && second.isKnockedOut()) {
            return RoundOutcome.DOUBLE_KNOCKOUT;
        }
        if (first.isKnockedOut()) {
            return RoundOutcome.FIRST_KNOCKED_OUT;
        }
        if (second.isKnockedOut()) {
            return RoundOutcome.SECOND_KNOCKED_OUT;
        }
        return RoundOutcome.NO_KNOCKOUT;
    }
}
