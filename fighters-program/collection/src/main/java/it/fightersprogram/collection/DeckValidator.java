package it.fightersprogram.collection;

import java.util.List;

public interface DeckValidator {

    List<Violation> validate(long userId, List<DeckEntry> entries);
}
