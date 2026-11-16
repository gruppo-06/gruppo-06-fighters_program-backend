package it.fightersprogram;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;

final class TestDecks {

    private TestDecks() {
    }

    static List<Long> cardIds(JdbcTemplate jdbcTemplate, String rarity, int count) {
        return jdbcTemplate.queryForList("""
                SELECT c.id FROM card c JOIN rarity r ON r.id = c.rarity_id
                WHERE r.name = ? AND c.code NOT LIKE 'Z%' ORDER BY c.code LIMIT ?
                """, Long.class, rarity, count);
    }

    static long cardId(JdbcTemplate jdbcTemplate, String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM card WHERE code = ?", Long.class, code);
    }

    static List<Long> starterDeck(JdbcTemplate jdbcTemplate) {
        List<Long> deck = new ArrayList<>(cardIds(jdbcTemplate, "Rara", 2));
        deck.addAll(cardIds(jdbcTemplate, "Comune", 8));
        return deck;
    }

    static String body(String name, List<Long> cardIds) {
        String cards = cardIds.stream()
                .map(id -> "{\"cardId\": " + id + ", \"copies\": 1}")
                .collect(Collectors.joining(", "));
        return "{\"name\": \"" + name + "\", \"cards\": [" + cards + "]}";
    }
}
