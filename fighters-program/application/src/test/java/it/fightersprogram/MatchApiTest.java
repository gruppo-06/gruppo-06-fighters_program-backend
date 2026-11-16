package it.fightersprogram;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import it.fightersprogram.engine.MatchLog;
import it.fightersprogram.matches.MatchFinished;
import it.fightersprogram.matches.MatchSimulator;
import it.fightersprogram.matches.SnapshotCard;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "game.starter-collection.enabled=true")
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class MatchApiTest {

    private static final String MATCHES = "/api/matches";
    private static final TypeReference<List<SnapshotCard>> SNAPSHOT = new TypeReference<>() {
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MatchSimulator simulator;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private ApplicationEvents events;

    private String alice;
    private String bob;
    private long aliceDeck;
    private long bobDeck;

    @BeforeEach
    void twoPlayersWithADeckEach() throws Exception {
        alice = TestUsers.registerAndLogin(mockMvc, "mt_alice");
        bob = TestUsers.registerAndLogin(mockMvc, "mt_bob");
        aliceDeck = createDeck(alice);
        bobDeck = createDeck(bob);
    }

    @Test
    void playsAWholeChallengeFromInvitationToHistory() throws Exception {
        long matchId = challenge(alice, "mt_bob", aliceDeck)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.challengerNickname", is("mt_alice")))
                .andExpect(jsonPath("$.opponentNickname", is("mt_bob")))
                .andReturn().getResponse().getContentAsString().transform(MatchApiTest::idOf);

        mockMvc.perform(get(MATCHES + "/incoming").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is((int) matchId)));
        mockMvc.perform(get(MATCHES + "/outgoing").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get(MATCHES + "/incoming").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(jsonPath("$", hasSize(0)));

        accept(bob, matchId, bobDeck)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FINISHED")))
                .andExpect(jsonPath("$.log.turns.length()", greaterThan(0)))
                .andExpect(jsonPath("$.challengerDeck", hasSize(10)))
                .andExpect(jsonPath("$.opponentDeck", hasSize(10)));

        mockMvc.perform(get(MATCHES + "/" + matchId).header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.log.summary.outcome").exists());
        mockMvc.perform(get(MATCHES + "/history").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].opponentNickname", is("mt_bob")))
                .andExpect(jsonPath("$.content[0].result", anyOf(is("WIN"), is("LOSS"), is("DRAW"))));
        mockMvc.perform(get(MATCHES + "/incoming").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void theTwoPlayersSeeOppositeResults() throws Exception {
        long matchId = playedMatch();

        String aliceResult = resultInHistory(alice);
        String bobResult = resultInHistory(bob);

        assertEquals(Map.of("WIN", "LOSS", "LOSS", "WIN", "DRAW", "DRAW").get(aliceResult), bobResult);
        Long winnerId = jdbcTemplate.queryForObject("SELECT winner_id FROM match WHERE id = ?", Long.class, matchId);
        assertEquals(1, events.stream(MatchFinished.class)
                .filter(event -> event.matchId() == matchId && Objects.equals(event.winnerId(), winnerId))
                .count());
    }

    @Test
    void theStoredSeedAndSnapshotsReplayExactlyTheStoredMatch() throws Exception {
        long matchId = playedMatch();
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT seed, log::text AS log, challenger_deck_snapshot::text AS one,
                       opponent_deck_snapshot::text AS two
                FROM match WHERE id = ?
                """, matchId);

        MatchLog stored = jsonMapper.readValue((String) row.get("log"), MatchLog.class);
        MatchLog replayed = simulator.simulate(jsonMapper.readValue((String) row.get("one"), SNAPSHOT),
                jsonMapper.readValue((String) row.get("two"), SNAPSHOT), (Long) row.get("seed")).log();

        assertEquals(stored, replayed);
    }

    @Test
    void aThirdUserCannotSeeTheChallenge() throws Exception {
        long matchId = playedMatch();
        String carol = TestUsers.registerAndLogin(mockMvc, "mt_carol");

        mockMvc.perform(get(MATCHES + "/" + matchId).header(HttpHeaders.AUTHORIZATION, carol))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(MATCHES + "/history").header(HttpHeaders.AUTHORIZATION, carol))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void aPendingChallengeShowsNoTurnsYet() throws Exception {
        long matchId = idOf(challenge(alice, "mt_bob", aliceDeck).andReturn().getResponse().getContentAsString());

        mockMvc.perform(get(MATCHES + "/" + matchId).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.log", nullValue()))
                .andExpect(jsonPath("$.winnerNickname", nullValue()));
    }

    @Test
    void nobodyCanChallengeThemselves() throws Exception {
        challenge(alice, "mt_alice", aliceDeck)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    void anUnknownOpponentIsNotFound() throws Exception {
        challenge(alice, "mt_nobody", aliceDeck).andExpect(status().isNotFound());
    }

    @Test
    void theChallengerMustUseTheirOwnDeck() throws Exception {
        challenge(alice, "mt_bob", bobDeck).andExpect(status().isNotFound());
    }

    @Test
    void aDeckThatIsNoLongerValidCannotBeUsed() throws Exception {
        jdbcTemplate.update("""
                DELETE FROM user_card WHERE user_id = (SELECT id FROM app_user WHERE nickname = 'mt_alice')
                """);

        challenge(alice, "mt_bob", aliceDeck)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code", is("INVALID_DECK")));
    }

    @Test
    void onlyTheChallengedPlayerCanAccept() throws Exception {
        long matchId = idOf(challenge(alice, "mt_bob", aliceDeck).andReturn().getResponse().getContentAsString());
        String carol = TestUsers.registerAndLogin(mockMvc, "mt_carol");

        accept(alice, matchId, aliceDeck).andExpect(status().isForbidden());
        accept(carol, matchId, aliceDeck).andExpect(status().isNotFound());
    }

    @Test
    void theOpponentMustAcceptWithTheirOwnDeck() throws Exception {
        long matchId = idOf(challenge(alice, "mt_bob", aliceDeck).andReturn().getResponse().getContentAsString());

        accept(bob, matchId, aliceDeck).andExpect(status().isNotFound());
    }

    @Test
    void aDeclinedChallengeCannotBeAccepted() throws Exception {
        long matchId = idOf(challenge(alice, "mt_bob", aliceDeck).andReturn().getResponse().getContentAsString());

        mockMvc.perform(post(MATCHES + "/" + matchId + "/decline").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(MATCHES + "/" + matchId + "/decline").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DECLINED")));
        accept(bob, matchId, bobDeck).andExpect(status().isConflict());
    }

    @Test
    void onlyTheChallengerCanCancelAPendingChallenge() throws Exception {
        long matchId = idOf(challenge(alice, "mt_bob", aliceDeck).andReturn().getResponse().getContentAsString());

        mockMvc.perform(post(MATCHES + "/" + matchId + "/cancel").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(MATCHES + "/" + matchId + "/cancel").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
        mockMvc.perform(post(MATCHES + "/" + matchId + "/cancel").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isConflict());
    }

    @Test
    void aDeckWaitingInAChallengeCannotBeDeleted() throws Exception {
        challenge(alice, "mt_bob", aliceDeck).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/me/decks/" + aliceDeck).header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isConflict());
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(get(MATCHES + "/history")).andExpect(status().isUnauthorized());
    }

    private long playedMatch() throws Exception {
        long matchId = idOf(challenge(alice, "mt_bob", aliceDeck).andReturn().getResponse().getContentAsString());
        accept(bob, matchId, bobDeck).andExpect(status().isOk());
        return matchId;
    }

    private String resultInHistory(String token) throws Exception {
        String body = mockMvc.perform(get(MATCHES + "/history").header(HttpHeaders.AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.content[0].result");
    }

    private long createDeck(String token) throws Exception {
        String body = mockMvc.perform(post("/api/me/decks").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestDecks.body("Main", TestDecks.starterDeck(jdbcTemplate))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    private ResultActions challenge(String token, String opponentNickname, long deckId) throws Exception {
        return mockMvc.perform(post(MATCHES).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"opponentNickname\": \"%s\", \"deckId\": %d}".formatted(opponentNickname, deckId)));
    }

    private ResultActions accept(String token, long matchId, long deckId) throws Exception {
        return mockMvc.perform(post(MATCHES + "/" + matchId + "/accept").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deckId\": %d}".formatted(deckId)));
    }

    private static long idOf(String body) {
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
