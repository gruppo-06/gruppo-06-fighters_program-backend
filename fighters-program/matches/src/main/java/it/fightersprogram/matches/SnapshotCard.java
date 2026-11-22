package it.fightersprogram.matches;

import java.util.List;

/**
 * @param vel       null in snapshots stored before the advanced mode
 * @param crt       null in snapshots stored before the advanced mode
 * @param subcards  codes of the sub-cards attached to the card; null in snapshots stored before the sub-cards
 */
public record SnapshotCard(String code, String name, String faction, String rarity, int atk, int def, Integer vel,
        Integer crt, List<String> subcards) {
}
