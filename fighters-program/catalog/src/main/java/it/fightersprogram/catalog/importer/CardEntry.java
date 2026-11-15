package it.fightersprogram.catalog.importer;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One card as written by the DSG in the catalog JSON. Field names follow the file, not the database.
 *
 * @param id   card code, e.g. {@code EA16}
 * @param copy flavor text shown on the card
 */
public record CardEntry(
        String id,
        String name,
        String country,
        @JsonProperty("class") String fighterClass,
        String rarity,
        CardStats stats,
        String copy,
        String image) {
}
