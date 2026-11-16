package it.fightersprogram;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@SpringBootTest(properties = "game.starter-collection.enabled=true")
@AutoConfigureMockMvc
@Transactional
class DeckValidationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void registerPlayer() throws Exception {
        token = TestUsers.registerAndLogin(mockMvc, "dv_player");
    }

    @Test
    void aDeckBuiltFromTheStarterCardsIsValid() throws Exception {
        validate(TestDecks.body("Starter", TestDecks.starterDeck(jdbcTemplate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.violations", hasSize(0)));
    }

    @Test
    void aCardThatIsNotOwnedMakesTheDeckInvalid() throws Exception {
        List<Long> deck = new ArrayList<>(TestDecks.cardIds(jdbcTemplate, "Comune", 9));
        deck.add(TestDecks.cardId(jdbcTemplate, "EA16"));

        validate(TestDecks.body("Borrowed", deck))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.violations[0].code", is("NOT_OWNED")))
                .andExpect(jsonPath("$.violations[0].message", is("Ti manca 1 copia di Hana Kuroda - Capitana")));
    }

    @Test
    void anUnknownCardIsReported() throws Exception {
        List<Long> deck = new ArrayList<>(TestDecks.cardIds(jdbcTemplate, "Comune", 9));
        deck.add(-1L);

        validate(TestDecks.body("Ghost", deck))
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.violations[*].code", hasItem("UNKNOWN_CARD")));
    }

    @Test
    void theSameCardOnTwoRowsIsReported() throws Exception {
        List<Long> deck = new ArrayList<>(TestDecks.cardIds(jdbcTemplate, "Comune", 9));
        deck.add(deck.getFirst());

        validate(TestDecks.body("Twice", deck))
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.violations[0].code", is("DUPLICATE_CARD")));
    }

    @Test
    void zeroCopiesIsABadRequest() throws Exception {
        long cardId = TestDecks.cardIds(jdbcTemplate, "Comune", 1).getFirst();

        validate("{\"name\": \"Zero\", \"cards\": [{\"cardId\": " + cardId + ", \"copies\": 0}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void validatingDoesNotSaveTheDeck() throws Exception {
        validate(TestDecks.body("Starter", TestDecks.starterDeck(jdbcTemplate))).andExpect(status().isOk());

        assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT count(*) FROM deck d JOIN app_user u ON u.id = d.user_id WHERE u.nickname = 'dv_player'
                """, Integer.class));
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(post("/api/me/decks/validate").contentType(MediaType.APPLICATION_JSON)
                        .content(TestDecks.body("Starter", List.of())))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions validate(String body) throws Exception {
        return mockMvc.perform(post("/api/me/decks/validate").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
