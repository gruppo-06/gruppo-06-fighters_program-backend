package it.fightersprogram;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = {"game.starter-collection.enabled=true", "game.deck.max-per-user=2"})
@AutoConfigureMockMvc
@Transactional
class DeckApiTest {

    private static final String DECKS = "/api/me/decks";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String alice;

    @BeforeEach
    void registerPlayer() throws Exception {
        alice = TestUsers.registerAndLogin(mockMvc, "deck_alice");
    }

    @Test
    void savesAValidDeckAndListsIt() throws Exception {
        long deckId = createdDeckId(alice, "Main");

        mockMvc.perform(get(DECKS).header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Main")))
                .andExpect(jsonPath("$[0].cardCount", is(10)));
        mockMvc.perform(get(DECKS + "/" + deckId).header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) deckId)))
                .andExpect(jsonPath("$.cards", hasSize(10)))
                .andExpect(jsonPath("$.cards[0].code").exists())
                .andExpect(jsonPath("$.cards[0].copies", is(1)));
    }

    @Test
    void anInvalidDeckIsRejectedWith422AndNotSaved() throws Exception {
        List<Long> deck = new ArrayList<>(TestDecks.cardIds(jdbcTemplate, "Comune", 9));
        deck.add(TestDecks.cardId(jdbcTemplate, "EA16"));

        create(alice, TestDecks.body("Borrowed", deck))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code", is("INVALID_DECK")))
                .andExpect(jsonPath("$.details[0]", startsWith("NOT_OWNED")));
        assertEquals(0, decksOf("deck_alice"));
    }

    @Test
    void theNameIsRequired() throws Exception {
        create(alice, TestDecks.body(" ", TestDecks.starterDeck(jdbcTemplate)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theNameIsUniquePerUser() throws Exception {
        createdDeckId(alice, "Main");

        create(alice, TestDecks.body("Main", TestDecks.starterDeck(jdbcTemplate)))
                .andExpect(status().isConflict());
    }

    @Test
    void aUserCannotGoOverTheDeckLimit() throws Exception {
        createdDeckId(alice, "One");
        createdDeckId(alice, "Two");

        create(alice, TestDecks.body("Three", TestDecks.starterDeck(jdbcTemplate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));
    }

    @Test
    void updatesNameAndCards() throws Exception {
        long deckId = createdDeckId(alice, "Main");
        List<Long> otherDeck = new ArrayList<>(TestDecks.cardIds(jdbcTemplate, "Comune", 10));

        update(alice, deckId, TestDecks.body("Renamed", otherDeck))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Renamed")))
                .andExpect(jsonPath("$.cards[*].cardId", hasItem(otherDeck.get(9).intValue())));
    }

    @Test
    void anInvalidUpdateKeepsTheSavedDeck() throws Exception {
        long deckId = createdDeckId(alice, "Main");

        update(alice, deckId, TestDecks.body("Broken", TestDecks.cardIds(jdbcTemplate, "Comune", 3)))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(get(DECKS + "/" + deckId).header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(jsonPath("$.name", is("Main")))
                .andExpect(jsonPath("$.cards", hasSize(10)));
    }

    @Test
    void deletesADeck() throws Exception {
        long deckId = createdDeckId(alice, "Main");

        mockMvc.perform(delete(DECKS + "/" + deckId).header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(DECKS + "/" + deckId).header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherUserCannotSeeChangeOrDeleteTheDeck() throws Exception {
        long deckId = createdDeckId(alice, "Main");
        String bob = TestUsers.registerAndLogin(mockMvc, "deck_bob");

        mockMvc.perform(get(DECKS + "/" + deckId).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNotFound());
        update(bob, deckId, TestDecks.body("Stolen", TestDecks.starterDeck(jdbcTemplate)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(DECKS + "/" + deckId).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(DECKS).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(jsonPath("$", hasSize(0)));
        assertEquals(1, decksOf("deck_alice"));
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(get(DECKS)).andExpect(status().isUnauthorized());
    }

    private long createdDeckId(String token, String name) throws Exception {
        String body = create(token, TestDecks.body(name, TestDecks.starterDeck(jdbcTemplate)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions create(String token, String body) throws Exception {
        return mockMvc.perform(post(DECKS).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions update(String token, long deckId, String body) throws Exception {
        return mockMvc.perform(put(DECKS + "/" + deckId).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int decksOf(String nickname) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM deck d JOIN app_user u ON u.id = d.user_id WHERE u.nickname = ?",
                Integer.class, nickname);
    }
}
