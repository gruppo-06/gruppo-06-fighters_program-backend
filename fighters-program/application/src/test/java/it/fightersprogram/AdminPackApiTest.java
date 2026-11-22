package it.fightersprogram;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPackApiTest {

    private static final String ADMIN_PACKS = "/api/admin/packs";
    private static final String KEY_HEADER = "Idempotency-Key";
    private static final String BASE_PACK = "Pacchetto Base";
    private static final String NEW_PACK = "Pacchetto Prova";
    private static final String BASE_CONTENTS = contents("Comune", 4, "Rara", 1);
    private static final String NEW_CONTENTS = contents("Comune", 3, "Rara", 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private String admin;
    private String player;

    @BeforeEach
    void logIn() throws Exception {
        admin = TestUsers.registerAndLoginAsAdmin(mockMvc, entityManager, "apack_admin");
        player = TestUsers.registerAndLogin(mockMvc, "apack_player");
    }

    @Test
    void anAdminCreatesAPackThatPlayersCanBuy() throws Exception {
        String body = create(admin, packJson(NEW_PACK, 150, NEW_CONTENTS, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(NEW_PACK)))
                .andExpect(jsonPath("$.description", is("Test pack")))
                .andExpect(jsonPath("$.price", is(150)))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.totalCards", is(4)))
                .andExpect(jsonPath("$.contents[0].rarity", is("Comune")))
                .andExpect(jsonPath("$.contents[0].cardCount", is(3)))
                .andExpect(jsonPath("$.contents[1].rarity", is("Rara")))
                .andExpect(jsonPath("$.contents[1].cardCount", is(1)))
                .andReturn().getResponse().getContentAsString();
        long packId = ((Number) JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(get("/api/packs/" + packId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(NEW_PACK)))
                .andExpect(jsonPath("$.totalCards", is(4)));
        mockMvc.perform(get("/api/packs"))
                .andExpect(jsonPath("$[*].name", hasItem(NEW_PACK)));
    }

    @Test
    void anAdminEditsThePriceNameAndContentsOfAPack() throws Exception {
        long packId = packId(BASE_PACK);

        edit(admin, packId, packJson(BASE_PACK, 120, contents("Epica", 2), true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) packId)))
                .andExpect(jsonPath("$.name", is(BASE_PACK)))
                .andExpect(jsonPath("$.price", is(120)))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.totalCards", is(2)))
                .andExpect(jsonPath("$.contents.length()", is(1)))
                .andExpect(jsonPath("$.contents[0].rarity", is("Epica")))
                .andExpect(jsonPath("$.contents[0].cardCount", is(2)));

        mockMvc.perform(get("/api/packs/" + packId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price", is(120)))
                .andExpect(jsonPath("$.contents.length()", is(1)))
                .andExpect(jsonPath("$.contents[0].rarity", is("Epica")));
        assertEquals(1, contentCount(packId));
    }

    @Test
    void theAdminListShowsInactivePacksWithTheirFlag() throws Exception {
        long packId = packId(BASE_PACK);
        deactivate(admin, packId).andExpect(status().isNoContent());

        list(admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(BASE_PACK)))
                .andExpect(jsonPath("$[?(@.name == '" + BASE_PACK + "')].active", hasItem(false)))
                .andExpect(jsonPath("$[?(@.name == 'Pacchetto Avanzato')].active", hasItem(true)));
    }

    @Test
    void deactivatingAPackHidesItButKeepsItsOpenings() throws Exception {
        long packId = packId(BASE_PACK);
        long userId = userId("apack_player");
        jdbcTemplate.update("""
                INSERT INTO pack_opening (user_id, pack_id, cards, idempotency_key)
                VALUES (?, ?, '[]'::jsonb, 'apack-history')
                """, userId, packId);

        deactivate(admin, packId).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/packs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", not(hasItem(BASE_PACK))));
        mockMvc.perform(get("/api/packs/" + packId)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        openPack(player, packId, "apack-after-deactivation").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        assertEquals(1, openingCount(packId));
    }

    @Test
    void reactivatingAPackPutsItBackOnSale() throws Exception {
        long packId = packId(BASE_PACK);
        deactivate(admin, packId).andExpect(status().isNoContent());

        edit(admin, packId, packJson(BASE_PACK, 100, BASE_CONTENTS, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
        mockMvc.perform(get("/api/packs/" + packId)).andExpect(status().isNotFound());

        edit(admin, packId, packJson(BASE_PACK, 100, BASE_CONTENTS, true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)));
        mockMvc.perform(get("/api/packs/" + packId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(BASE_PACK)));
        mockMvc.perform(get("/api/packs"))
                .andExpect(jsonPath("$[*].name", hasItem(BASE_PACK)));
    }

    @Test
    void anEditCanDeactivateAPackWithoutDeletingIt() throws Exception {
        long packId = packId(BASE_PACK);

        edit(admin, packId, packJson(BASE_PACK, 100, BASE_CONTENTS, false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
        mockMvc.perform(get("/api/packs/" + packId)).andExpect(status().isNotFound());
    }

    @Test
    void aDeletedPackIsDeactivatedNotRemoved() throws Exception {
        long packId = packId(BASE_PACK);

        deactivate(admin, packId).andExpect(status().isNoContent());
        entityManager.flush();

        assertEquals(1, packCount(BASE_PACK));
        assertEquals(2, contentCount(packId));
        assertEquals(false, jdbcTemplate.queryForObject("SELECT active FROM pack WHERE id = ?", Boolean.class,
                packId));
    }

    @Test
    void invalidPacksAreRejectedAsValidationErrors() throws Exception {
        create(admin, packJson("   ", 100, BASE_CONTENTS, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.details", hasItem("name: must not be blank")));
        create(admin, packJson("x".repeat(51), 100, BASE_CONTENTS, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        create(admin, packJson(NEW_PACK, -1, BASE_CONTENTS, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        create(admin, packJson(NEW_PACK, null, BASE_CONTENTS, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        create(admin, packJson(NEW_PACK, 100, "[]", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        create(admin, packJson(NEW_PACK, 100, contents("Comune", 0), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        create(admin, """
                {"name": "%s", "description": "%s", "price": 100, "contents": %s}
                """.formatted(NEW_PACK, "x".repeat(256), BASE_CONTENTS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        assertEquals(0, packCount(NEW_PACK));
    }

    @Test
    void aRarityRepeatedInThePackIsABadRequest() throws Exception {
        create(admin, packJson(NEW_PACK, 100, contents("Comune", 2, "Comune", 1), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
        assertEquals(0, packCount(NEW_PACK));
    }

    @Test
    void anUnknownRarityIsUnprocessable() throws Exception {
        create(admin, packJson(NEW_PACK, 100, contents("Mitica", 1), null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code", is("UNKNOWN_RARITY")));
        assertEquals(0, packCount(NEW_PACK));
    }

    @Test
    void aRarityWithoutActiveCardsIsUnprocessable() throws Exception {
        jdbcTemplate.update(
                "UPDATE card SET active = FALSE WHERE rarity_id = (SELECT id FROM rarity WHERE name = 'Rara')");
        entityManager.clear();
        long packId = packId(BASE_PACK);

        create(admin, packJson(NEW_PACK, 100, NEW_CONTENTS, null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code", is("PACK_UNAVAILABLE")));
        edit(admin, packId, packJson(BASE_PACK, 100, BASE_CONTENTS, null))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code", is("PACK_UNAVAILABLE")));
        assertEquals(0, packCount(NEW_PACK));
        assertEquals(2, contentCount(packId));
    }

    @Test
    void aDuplicateNameIsAConflict() throws Exception {
        create(admin, packJson(BASE_PACK, 100, BASE_CONTENTS, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));

        create(admin, packJson(NEW_PACK, 150, NEW_CONTENTS, null)).andExpect(status().isCreated());
        long basePackId = packId(BASE_PACK);
        edit(admin, basePackId, packJson(NEW_PACK, 100, BASE_CONTENTS, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));
        assertEquals(BASE_PACK, jdbcTemplate.queryForObject("SELECT name FROM pack WHERE id = ?", String.class,
                basePackId));
    }

    @Test
    void anUnknownPackIsNotFound() throws Exception {
        edit(admin, 999_999L, packJson(BASE_PACK, 100, BASE_CONTENTS, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        deactivate(admin, 999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    void aPlayerCannotManagePacks() throws Exception {
        long packId = packId(BASE_PACK);

        list(player).andExpect(status().isForbidden());
        create(player, packJson(NEW_PACK, 100, BASE_CONTENTS, null)).andExpect(status().isForbidden());
        edit(player, packId, packJson(BASE_PACK, 1, BASE_CONTENTS, null)).andExpect(status().isForbidden());
        deactivate(player, packId).andExpect(status().isForbidden());
        assertEquals(0, packCount(NEW_PACK));
        assertEquals(1, packCount(BASE_PACK));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(ADMIN_PACKS)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ADMIN_PACKS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(packJson(NEW_PACK, 100, BASE_CONTENTS, null)))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions list(String token) throws Exception {
        return mockMvc.perform(get(ADMIN_PACKS).header(HttpHeaders.AUTHORIZATION, token));
    }

    private ResultActions create(String token, String body) throws Exception {
        return mockMvc.perform(post(ADMIN_PACKS)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions edit(String token, long packId, String body) throws Exception {
        return mockMvc.perform(put(ADMIN_PACKS + "/" + packId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions deactivate(String token, long packId) throws Exception {
        return mockMvc.perform(delete(ADMIN_PACKS + "/" + packId).header(HttpHeaders.AUTHORIZATION, token));
    }

    private ResultActions openPack(String token, long packId, String key) throws Exception {
        return mockMvc.perform(post("/api/packs/{id}/open", packId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .header(KEY_HEADER, key));
    }

    private long packId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM pack WHERE name = ?", Long.class, name);
    }

    private long userId(String nickname) {
        return jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE nickname = ?", Long.class, nickname);
    }

    private int packCount(String name) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pack WHERE name = ?", Integer.class, name);
    }

    private int contentCount(long packId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pack_content WHERE pack_id = ?", Integer.class,
                packId);
    }

    private int openingCount(long packId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pack_opening WHERE pack_id = ?", Integer.class,
                packId);
    }

    private static String contents(Object... rarityAndCount) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < rarityAndCount.length; i += 2) {
            if (i > 0) {
                json.append(", ");
            }
            json.append("{\"rarity\": \"%s\", \"cardCount\": %d}".formatted(rarityAndCount[i], rarityAndCount[i + 1]));
        }
        return json.append("]").toString();
    }

    private static String packJson(String name, Integer price, String contents, Boolean active) {
        return """
                {"name": "%s", "description": "Test pack", "price": %s, "contents": %s, "active": %s}
                """.formatted(name, price == null ? "null" : price, contents,
                active == null ? "null" : active);
    }
}
