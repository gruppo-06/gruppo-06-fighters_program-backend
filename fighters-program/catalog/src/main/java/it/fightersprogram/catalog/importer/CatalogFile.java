package it.fightersprogram.catalog.importer;

import java.util.List;

public record CatalogFile(int schemaVersion, String faction, List<CardEntry> cards) {

    public CatalogFile {
        cards = cards == null ? List.of() : List.copyOf(cards);
    }
}
