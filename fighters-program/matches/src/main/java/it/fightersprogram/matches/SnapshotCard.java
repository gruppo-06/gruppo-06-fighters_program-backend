package it.fightersprogram.matches;

/**
 * @param vel null in snapshots stored before the advanced mode
 * @param crt null in snapshots stored before the advanced mode
 */
public record SnapshotCard(String code, String name, String faction, String rarity, int atk, int def, Integer vel,
        Integer crt) {
}
