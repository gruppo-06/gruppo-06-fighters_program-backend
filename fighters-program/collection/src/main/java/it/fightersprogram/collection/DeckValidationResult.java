package it.fightersprogram.collection;

import java.util.List;

public record DeckValidationResult(boolean valid, List<Violation> violations) {

    public static DeckValidationResult of(List<Violation> violations) {
        return new DeckValidationResult(violations.isEmpty(), violations);
    }
}
