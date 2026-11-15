package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CollectionAndDeckSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long userId;
    private long cardId;

    @BeforeEach
    void insertUserAndCard() {
        userId = jdbcTemplate.queryForObject("""
                INSERT INTO app_user (nickname, email, password_hash)
                VALUES ('schema_test', 'schema_test@example.com', 'hash')
                RETURNING id
                """, Long.class);
        cardId = jdbcTemplate.queryForObject("""
                INSERT INTO card (code, name, faction_id, class_id, rarity_id, atk, def, vel, crt)
                VALUES ('ZZ99', 'Schema Test Card', 1, 1, 1, 100, 100, 100, 100)
                RETURNING id
                """, Long.class);
    }

    @Test
    void ownedCardQuantityMustBeAtLeastOne() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO user_card (user_id, card_id, quantity) VALUES (?, ?, 0)", userId, cardId));
    }

    @Test
    void sameCardCannotBeOwnedInTwoRows() {
        jdbcTemplate.update("INSERT INTO user_card (user_id, card_id, quantity) VALUES (?, ?, 1)", userId, cardId);

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO user_card (user_id, card_id, quantity) VALUES (?, ?, 2)", userId, cardId));
    }

    @Test
    void deckNameIsUniquePerUser() {
        insertDeck("Main");

        assertThrows(DataIntegrityViolationException.class, () -> insertDeck("Main"));
    }

    @Test
    void sameCardCannotAppearTwiceInTheSameDeck() {
        long deckId = insertDeck("Main");
        jdbcTemplate.update("INSERT INTO deck_card (deck_id, card_id, copies) VALUES (?, ?, 1)", deckId, cardId);

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO deck_card (deck_id, card_id, copies) VALUES (?, ?, 1)", deckId, cardId));
    }

    @Test
    void deckCardCopiesMustBeAtLeastOne() {
        long deckId = insertDeck("Main");

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO deck_card (deck_id, card_id, copies) VALUES (?, ?, 0)", deckId, cardId));
    }

    @Test
    void deletingDeckDeletesItsCards() {
        long deckId = insertDeck("Main");
        jdbcTemplate.update("INSERT INTO deck_card (deck_id, card_id, copies) VALUES (?, ?, 1)", deckId, cardId);

        jdbcTemplate.update("DELETE FROM deck WHERE id = ?", deckId);

        Integer remainingDeckCards = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM deck_card WHERE deck_id = ?", Integer.class, deckId);
        assertEquals(0, remainingDeckCards);
    }

    private long insertDeck(String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO deck (user_id, name) VALUES (?, ?) RETURNING id", Long.class, userId, name);
    }
}
