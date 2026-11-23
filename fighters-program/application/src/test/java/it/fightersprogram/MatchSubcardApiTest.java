package it.fightersprogram;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import it.fightersprogram.engine.BaseRules;

class MatchSubcardApiTest {

    private static final String MATCHES = "/api/matches";
    private static final String OPPONENT_NICKNAME = "msc_bob";
    private static final String SUBCARD = "SC01";
    private static final int SUBCARD_ATK_PERCENT = 12;

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

    private static long createDeck(MockMvc mockMvc, JdbcTemplate jdbcTemplate, String token, boolean withSubcards)
            throws Exception {
        String body = mockMvc.perform(post("/api/me/decks").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(TestDecks.starterDeck(jdbcTemplate), withSubcards)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String body(List<Long> cardIds, boolean withSubcards) {
        String subcards = withSubcards ? ", \"subcards\": [\"%s\"]".formatted(SUBCARD) : "";
        String cards = cardIds.stream()
                .map(cardId -> "{\"cardId\": %d, \"copies\": 1%s}".formatted(cardId, subcards))
                .collect(Collectors.joining(", "));
        return "{\"name\": \"Main\", \"cards\": [" + cards + "]}";
    }

    private static long matchId(String body) {
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static int expectedAttack(int printed, int modifierPercent, boolean factionAdvantage,
            int factionBonusPercent) {
        int attack = (printed * (100 + modifierPercent) + 50) / 100;
        return factionAdvantage ? (attack * (100 + factionBonusPercent) + 50) / 100 : attack;
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

        @Autowired
        private BaseRules rules;

        private String alice;
        private String bob;
        private long aliceDeck;
        private long bobDeck;

        @BeforeEach
        void twoPlayersWithADeckEach() throws Exception {
            alice = TestUsers.registerAndLogin(mockMvc, "msc_alice");
            bob = TestUsers.registerAndLogin(mockMvc, OPPONENT_NICKNAME);
            aliceDeck = createDeck(mockMvc, jdbcTemplate, alice, true);
            bobDeck = createDeck(mockMvc, jdbcTemplate, bob, false);
        }

        @Test
        void anAdvancedMatchAppliesTheSubcardsToTheEffectiveAttack() throws Exception {
            long matchId = matchId(challenge(mockMvc, alice, aliceDeck, "ADVANCED")
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString());

            String body = accept(mockMvc, bob, matchId, bobDeck)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("FINISHED")))
                    .andExpect(jsonPath("$.challengerDeck[0].subcards[0]", is(SUBCARD)))
                    .andReturn().getResponse().getContentAsString();

            assertFightersMatch(body, SUBCARD_ATK_PERCENT);
        }

        @Test
        void aBaseMatchPlaysWithThePrintedStats() throws Exception {
            long matchId = matchId(challenge(mockMvc, alice, aliceDeck, null)
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString());

            String body = accept(mockMvc, bob, matchId, bobDeck)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mode", is("BASE")))
                    .andExpect(jsonPath("$.challengerDeck[0].subcards[0]", is(SUBCARD)))
                    .andReturn().getResponse().getContentAsString();

            assertFightersMatch(body, 0);
        }

        private void assertFightersMatch(String body, int modifierPercent) {
            List<Map<String, Object>> deck = JsonPath.read(body, "$.challengerDeck");
            Map<String, Integer> printedAttack = deck.stream()
                    .collect(Collectors.toMap(card -> (String) card.get("code"),
                            card -> ((Number) card.get("atk")).intValue()));
            List<Map<String, Object>> fighters = JsonPath.read(body, "$.log.turns[*].fighterOne");
            assertFalse(fighters.isEmpty());

            for (Map<String, Object> fighter : fighters) {
                String code = (String) fighter.get("cardCode");
                boolean advantage = (Boolean) fighter.get("hasFactionAdvantage");
                int effective = ((Number) fighter.get("effectiveAttack")).intValue();
                assertEquals(expectedAttack(printedAttack.get(code), modifierPercent, advantage,
                        rules.factionBonusPercent()), effective, code);
            }
        }
    }
}
