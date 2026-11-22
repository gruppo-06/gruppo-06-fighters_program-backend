package it.fightersprogram;

import static org.hamcrest.Matchers.is;
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

class AdvancedMatchApiTest {

    private static final String MATCHES = "/api/matches";
    private static final String OPPONENT_NICKNAME = "am_bob";

    private static ResultActions challenge(MockMvc mockMvc, String token, long deckId, String mode)
            throws Exception {
        String modeField = mode == null ? "" : ", \"mode\": \"%s\"".formatted(mode);
        return mockMvc.perform(post(MATCHES).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"opponentNickname\": \"%s\", \"deckId\": %d%s}"
                        .formatted(OPPONENT_NICKNAME, deckId, modeField)));
    }

    private static ResultActions accept(MockMvc mockMvc, String token, long matchId, long deckId) throws Exception {
        return mockMvc.perform(post(MATCHES + "/" + matchId + "/accept").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deckId\": %d}".formatted(deckId)));
    }

    private static long createDeck(MockMvc mockMvc, JdbcTemplate jdbcTemplate, String token) throws Exception {
        String body = mockMvc.perform(post("/api/me/decks").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestDecks.body("Main", TestDecks.starterDeck(jdbcTemplate))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Nested
    @SpringBootTest(properties = "game.modes.advanced.enabled=true")
    @AutoConfigureMockMvc
    @Transactional
    class WhenTheAdvancedModeIsEnabled {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private String alice;
        private String bob;
        private long aliceDeck;
        private long bobDeck;

        @BeforeEach
        void twoPlayersWithADeckEach() throws Exception {
            alice = TestUsers.registerAndLogin(mockMvc, "am_alice");
            bob = TestUsers.registerAndLogin(mockMvc, OPPONENT_NICKNAME);
            aliceDeck = createDeck(mockMvc, jdbcTemplate, alice);
            bobDeck = createDeck(mockMvc, jdbcTemplate, bob);
        }

        @Test
        void anAdvancedChallengeIsAccepted() throws Exception {
            challenge(mockMvc, alice, aliceDeck, "ADVANCED")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mode", is("ADVANCED")));
        }

        @Test
        void anAdvancedMatchIsReplayedWithItsModeAndCombatStats() throws Exception {
            long matchId = JsonPath.<Number>read(challenge(mockMvc, alice, aliceDeck, "ADVANCED")
                    .andReturn().getResponse().getContentAsString(), "$.id").longValue();

            accept(mockMvc, bob, matchId, bobDeck)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mode", is("ADVANCED")))
                    .andExpect(jsonPath("$.log.mode", is("ADVANCED")))
                    .andExpect(jsonPath("$.challengerDeck[0].vel").isNumber())
                    .andExpect(jsonPath("$.challengerDeck[0].crt").isNumber())
                    .andExpect(jsonPath("$.opponentDeck[0].vel").isNumber())
                    .andExpect(jsonPath("$.opponentDeck[0].crt").isNumber());
        }
    }

    @Nested
    @SpringBootTest(properties = "game.modes.advanced.enabled=false")
    @AutoConfigureMockMvc
    @Transactional
    class WhenTheAdvancedModeIsDisabled {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private String alice;
        private String bob;
        private long aliceDeck;
        private long bobDeck;

        @BeforeEach
        void twoPlayersWithADeckEach() throws Exception {
            alice = TestUsers.registerAndLogin(mockMvc, "am_alice");
            bob = TestUsers.registerAndLogin(mockMvc, OPPONENT_NICKNAME);
            aliceDeck = createDeck(mockMvc, jdbcTemplate, alice);
            bobDeck = createDeck(mockMvc, jdbcTemplate, bob);
        }

        @Test
        void anAdvancedChallengeIsRefused() throws Exception {
            challenge(mockMvc, alice, aliceDeck, "ADVANCED")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                    .andExpect(jsonPath("$.message", is("The advanced mode is not enabled")));
        }

        @Test
        void aChallengeWithoutAModeIsABaseMatch() throws Exception {
            long matchId = JsonPath.<Number>read(challenge(mockMvc, alice, aliceDeck, null)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mode", is("BASE")))
                    .andReturn().getResponse().getContentAsString(), "$.id").longValue();

            accept(mockMvc, bob, matchId, bobDeck)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mode", is("BASE")))
                    .andExpect(jsonPath("$.log.mode", is("BASE")));
        }
    }
}
