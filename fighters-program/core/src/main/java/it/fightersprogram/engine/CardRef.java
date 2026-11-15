package it.fightersprogram.engine;

/**
 * @param deckPosition zero-based position in the shuffled deck; tells apart copies of the same card
 */
public record CardRef(int deckPosition, String code, String name) {
}
