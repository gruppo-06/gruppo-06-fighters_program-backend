package it.fightersprogram.economy;

import java.util.List;

public record PackOpeningResult(long openingId, long packId, String packName, int price, List<OpenedCard> cards,
        long balance) {
}
