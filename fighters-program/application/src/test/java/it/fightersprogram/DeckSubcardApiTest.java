package it.fightersprogram;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = "game.starter-collection.enabled=true")
@AutoConfigureMockMvc
@Transactional
class DeckSubcardApiTest {

    private static final String DECKS = "/api/me/decks";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private String token;
    private List<Long> starter;
    private long common;
    private long rare;

    @BeforeEach
    void registerPlayer() throws Exception {
        token = TestUsers.registerAndLogin(mockMvc, "ds_player");
        starter = TestDecks.starterDeck(jdbcTemplate);
        common = TestDecks.cardIds(jdbcTemplate, "Comune", 1).getFirst();
        rare = TestDecks.cardIds(jdbcTemplate, "Rara", 1).getFirst();
    }

    @Test
    void savesThreeSubcardsOnACommonAndReadsThemBackWithTheirModifiers() throws Exception {
        String created = create(body("Subcards", starter, Map.of(common, List.of("SC01", "SC02", "SC03"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long deckId = number(created, "$.id").longValue();

        String loaded = mockMvc.perform(get(DECKS + "/" + deckId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> subcards = subcardsOf(cardOf(loaded, common));
        assertEquals(List.of("SC01", "SC02", "SC03"), subcards.stream().map(subcard -> subcard.get("code")).toList());
        Map<String, Map<String, Object>> byCode = subcards.stream()
                .collect(Collectors.toMap(subcard -> (String) subcard.get("code"), subcard -> subcard));
        assertSubcard(byCode.get("SC01"), "SC01", "Guanti pesanti", 12, 0, -8, 0);
        assertSubcard(byCode.get("SC02"), "SC02", "Corazza leggera", 0, 12, -6, 0);
        assertSubcard(byCode.get("SC03"), "SC03", "Adrenalina", 0, -8, 15, 0);
        assertEquals(3, storedSubcards(deckId, common));
    }

    @ParameterizedTest(name = "{0} with {1} sub-cards")
    @CsvSource({"Comune, 4", "Rara, 3", "Epica, 2", "Leggendaria, 1"})
    void rejectsTooManySubcardsForTheCardRarity(String rarity, int count) throws Exception {
        long cardId = cardOfRarity(rarity);

        create(body("Too many", deckWith(cardId), Map.of(cardId, codes(count))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code", is("INVALID_DECK")))
                .andExpect(jsonPath("$.details[0]", is("TOO_MANY_SUBCARDS: "
                        + TestDecks.cardName(jdbcTemplate, cardId) + " può avere al massimo " + slotsOf(rarity)
                        + " sottocarte (rarità " + rarity + "), ne ha " + count)));
        assertEquals(0, decksOfPlayer());
    }

    @Test
    void rejectsADuplicateSubcardOnTheSameCard() throws Exception {
        create(body("Duplicate", starter, Map.of(common, List.of("SC01", "SC01"))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.details[0]", is("DUPLICATE_SUBCARD: "
                        + TestDecks.cardName(jdbcTemplate, common) + " ha la sottocarta SC01 più di una volta")));
        assertEquals(0, decksOfPlayer());
    }

    @Test
    void rejectsAnUnknownSubcard() throws Exception {
        create(body("Unknown", starter, Map.of(common, List.of("SC99"))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.details[0]", is("UNKNOWN_SUBCARD: La sottocarta SC99 non esiste")));
        assertEquals(0, decksOfPlayer());
    }

    @Test
    void updatingADeckReplacesItsSubcards() throws Exception {
        String created = create(body("Editable", starter, Map.of(common, List.of("SC01", "SC02", "SC03"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long deckId = number(created, "$.id").longValue();

        String updated = update(deckId, body("Editable", starter, Map.of(common, List.of("SC04"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> subcards = subcardsOf(cardOf(updated, common));

        assertEquals(1, subcards.size());
        assertSubcard(subcards.getFirst(), "SC04", "Mirino tattico", -6, 0, 0, 20);
        assertEquals(1, storedSubcards(deckId, common));
        assertEquals("SC04", jdbcTemplate.queryForObject("""
                SELECT s.code FROM deck_card_subcard dcs JOIN subcard s ON s.id = dcs.subcard_id
                WHERE dcs.deck_id = ? AND dcs.card_id = ?
                """, String.class, deckId, common));

        String overlapping = update(deckId, body("Editable", starter, Map.of(common, List.of("SC04", "SC01"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(2, subcardsOf(cardOf(overlapping, common)).size());
        assertEquals(2, storedSubcards(deckId, common));

        String cleared = update(deckId, body("Editable", starter, Map.of()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(0, subcardsOf(cardOf(cleared, common)).size());
        assertEquals(0, storedSubcards(deckId, common));
    }

    @Test
    void validateAlsoReportsSubcardProblems() throws Exception {
        validate(body("Check", starter, Map.of(common, List.of("SC99"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.violations[0].code", is("UNKNOWN_SUBCARD")));
        validate(body("Check", starter, Map.of(common, List.of("SC01", "SC01"))))
                .andExpect(jsonPath("$.violations[0].code", is("DUPLICATE_SUBCARD")));
        validate(body("Check", starter, Map.of(common, codes(4))))
                .andExpect(jsonPath("$.violations[0].code", is("TOO_MANY_SUBCARDS")));
        assertEquals(0, decksOfPlayer());
    }

    private long cardOfRarity(String rarity) {
        return switch (rarity) {
            case "Comune" -> common;
            case "Rara" -> rare;
            case "Epica" -> TestDecks.cardIds(jdbcTemplate, "Epica", 1).getFirst();
            case "Leggendaria" -> TestDecks.legendaryCardId(jdbcTemplate, "Euroatlantica");
            default -> throw new IllegalArgumentException("Unknown rarity " + rarity);
        };
    }

    private List<Long> deckWith(long cardId) {
        List<Long> cards = new ArrayList<>(starter);
        if (!cards.contains(cardId)) {
            jdbcTemplate.update("""
                    INSERT INTO user_card (user_id, card_id, quantity)
                    SELECT id, ?, 1 FROM app_user WHERE nickname = 'ds_player'
                    ON CONFLICT (user_id, card_id) DO UPDATE SET quantity = EXCLUDED.quantity
                    """, cardId);
            cards.removeLast();
            cards.add(cardId);
        }
        return cards;
    }

    private static int slotsOf(String rarity) {
        return switch (rarity) {
            case "Comune" -> 3;
            case "Rara" -> 2;
            case "Epica" -> 1;
            case "Leggendaria" -> 0;
            default -> throw new IllegalArgumentException("Unknown rarity " + rarity);
        };
    }

    private static List<String> codes(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(index -> "SC%02d".formatted(index)).toList();
    }

    private static String body(String name, List<Long> cardIds, Map<Long, List<String>> subcards) {
        String cards = cardIds.stream().map(cardId -> {
            List<String> codes = subcards.getOrDefault(cardId, List.of());
            String subcardFields = codes.isEmpty() ? ""
                    : ", \"subcards\": [" + codes.stream().map(code -> "\"" + code + "\"")
                            .collect(Collectors.joining(", ")) + "]";
            return "{\"cardId\": " + cardId + ", \"copies\": 1" + subcardFields + "}";
        }).collect(Collectors.joining(", "));
        return "{\"name\": \"" + name + "\", \"cards\": [" + cards + "]}";
    }

    private ResultActions create(String body) throws Exception {
        return mockMvc.perform(post(DECKS).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions update(long deckId, String body) throws Exception {
        return mockMvc.perform(put(DECKS + "/" + deckId).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions validate(String body) throws Exception {
        return mockMvc.perform(post(DECKS + "/validate").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int storedSubcards(long deckId, long cardId) {
        entityManager.flush();
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM deck_card_subcard WHERE deck_id = ? AND card_id = ?
                """, Integer.class, deckId, cardId);
    }

    private int decksOfPlayer() {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM deck d JOIN app_user u ON u.id = d.user_id WHERE u.nickname = 'ds_player'
                """, Integer.class);
    }

    private static Number number(String json, String path) {
        return JsonPath.read(json, path);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cardOf(String json, long cardId) {
        List<Map<String, Object>> cards = JsonPath.read(json, "$.cards");
        return cards.stream()
                .filter(card -> ((Number) card.get("cardId")).longValue() == cardId)
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> subcardsOf(Map<String, Object> card) {
        return (List<Map<String, Object>>) card.get("subcards");
    }

    private static void assertSubcard(Map<String, Object> subcard, String code, String name,
            int atk, int def, int vel, int crt) {
        assertEquals(code, subcard.get("code"));
        assertEquals(name, subcard.get("name"));
        assertEquals(atk, ((Number) subcard.get("atk")).intValue());
        assertEquals(def, ((Number) subcard.get("def")).intValue());
        assertEquals(vel, ((Number) subcard.get("vel")).intValue());
        assertEquals(crt, ((Number) subcard.get("crt")).intValue());
    }
}
