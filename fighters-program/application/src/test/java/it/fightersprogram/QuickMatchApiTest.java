package it.fightersprogram;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

@SpringBootTest(properties = "game.starter-collection.enabled=true")
@AutoConfigureMockMvc
@Transactional
class QuickMatchApiTest {

    private static final String QUICK = "/api/matches/quick";
    private static final String ALICE = "qm_alice";
    private static final String BOB = "qm_bob";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String alice;
    private long aliceDeck;

    @BeforeEach
    void aPlayerWithADeck() throws Exception {
        alice = TestUsers.registerAndLogin(mockMvc, ALICE);
        aliceDeck = createDeck(alice);
    }

    @Test
    void aQuickMatchIsPlayedAtOnceAgainstAnotherPlayer() throws Exception {
        TestUsers.registerAndLogin(mockMvc, BOB);
        createDeck(TestUsers.registerAndLogin(mockMvc, "qm_carol"));

        quick(alice, aliceDeck, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FINISHED")))
                .andExpect(jsonPath("$.challengerNickname", is(ALICE)))
                .andExpect(jsonPath("$.opponentNickname", not(is(ALICE))))
                .andExpect(jsonPath("$.log.turns.length()", greaterThan(0)))
                .andExpect(jsonPath("$.mode", is("BASE")));

        mockMvc.perform(get("/api/matches/history").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].opponentNickname", not(is(ALICE))));
    }

    @Test
    void theCallerIsNeverPickedAsTheirOwnOpponent() throws Exception {
        createDeck(TestUsers.registerAndLogin(mockMvc, BOB));

        for (int i = 0; i < 5; i++) {
            quick(alice, aliceDeck, null)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.opponentNickname", not(is(ALICE))));
        }
    }

    @Test
    void anInvalidCallerDeckIsRefused() throws Exception {
        createDeck(TestUsers.registerAndLogin(mockMvc, BOB));
        jdbcTemplate.update("""
                DELETE FROM user_card WHERE user_id = (SELECT id FROM app_user WHERE nickname = ?)
                """, ALICE);

        quick(alice, aliceDeck, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code", is("INVALID_DECK")));
    }

    @Test
    void anAdvancedQuickMatchIsRefusedWhenTheModeIsDisabled() throws Exception {
        createDeck(TestUsers.registerAndLogin(mockMvc, BOB));

        quick(alice, aliceDeck, "ADVANCED")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The advanced mode is not enabled")));
    }

    @Nested
    @SpringBootTest(properties = {"game.starter-collection.enabled=true", "game.modes.advanced.enabled=true"})
    @AutoConfigureMockMvc
    @Transactional
    class WhenTheAdvancedModeIsEnabled {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Test
        void anAdvancedQuickMatchIsReplayedAsAdvanced() throws Exception {
            String alice = TestUsers.registerAndLogin(mockMvc, "qma_alice");
            long aliceDeck = createDeck(mockMvc, jdbcTemplate, alice);
            createDeck(mockMvc, jdbcTemplate, TestUsers.registerAndLogin(mockMvc, "qma_bob"));

            quick(mockMvc, alice, aliceDeck, "ADVANCED")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status", is("FINISHED")))
                    .andExpect(jsonPath("$.mode", is("ADVANCED")))
                    .andExpect(jsonPath("$.log.mode", is("ADVANCED")));
        }
    }

    private ResultActions quick(String token, long deckId, String mode) throws Exception {
        return quick(mockMvc, token, deckId, mode);
    }

    private static ResultActions quick(MockMvc mockMvc, String token, long deckId, String mode) throws Exception {
        String modeField = mode == null ? "" : ", \"mode\": \"%s\"".formatted(mode);
        return mockMvc.perform(post(QUICK).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deckId\": %d%s}".formatted(deckId, modeField)));
    }

    private long createDeck(String token) throws Exception {
        return createDeck(mockMvc, jdbcTemplate, token);
    }

    private static long createDeck(MockMvc mockMvc, JdbcTemplate jdbcTemplate, String token) throws Exception {
        String body = mockMvc.perform(post("/api/me/decks").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestDecks.body("Main", TestDecks.starterDeck(jdbcTemplate))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
