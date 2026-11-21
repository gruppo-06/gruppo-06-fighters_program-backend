package it.fightersprogram;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

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

import it.fightersprogram.catalog.CatalogCard;
import it.fightersprogram.catalog.CatalogService;
import it.fightersprogram.collection.CollectionService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminCardApiTest {

    private static final String ADMIN_CARDS = "/api/admin/cards";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private CollectionService collectionService;

    private String admin;
    private String player;

    @BeforeEach
    void logIn() throws Exception {
        admin = TestUsers.registerAndLoginAsAdmin(mockMvc, entityManager, "acard_admin");
        player = TestUsers.registerAndLogin(mockMvc, "acard_player");
    }

    @Test
    void anAdminCreatesACardManagedByAdmins() throws Exception {
        create(admin, cardJson("ZX01", "Admin Fighter", "Rara", 300))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("ZX01")))
                .andExpect(jsonPath("$.name", is("Admin Fighter")))
                .andExpect(jsonPath("$.faction", is("Africana")))
                .andExpect(jsonPath("$.rarity", is("Rara")))
                .andExpect(jsonPath("$.atk", is(300)))
                .andExpect(jsonPath("$.active", is(true)));

        assertFlags("ZX01", true, true);
    }

    @Test
    void aPlayerCannotCreateCards() throws Exception {
        create(player, cardJson("ZX01", "Admin Fighter", "Rara", 300))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAlreadyUsedCodeIsAConflict() throws Exception {
        create(admin, cardJson("ZX01", "Admin Fighter", "Rara", 300)).andExpect(status().isCreated());

        create(admin, cardJson("ZX01", "Another Fighter", "Rara", 300))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidFieldsAreRejected() throws Exception {
        create(admin, cardJson("ZX01", "", "Rara", 300))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.details", hasItem("name: must not be blank")));
        create(admin, cardJson("ZX01", "x".repeat(101), "Rara", 300)).andExpect(status().isBadRequest());
        create(admin, cardJson("ZX01234567X", "Admin Fighter", "Rara", 300)).andExpect(status().isBadRequest());
        create(admin, cardJson("ZX01", "Admin Fighter", "Rara", 1001)).andExpect(status().isBadRequest());
        create(admin, cardJson("ZX01", "Admin Fighter", "Rara", -1)).andExpect(status().isBadRequest());
    }

    @Test
    void unknownReferencesAreUnprocessable() throws Exception {
        create(admin, cardJson("ZX01", "Admin Fighter", "Mitica", 300))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code", is("UNKNOWN_REFERENCE")));
        create(admin, cardJson("ZX01", "Admin Fighter", "Rara", 300).replace("Assassini", "Maghi"))
                .andExpect(status().isUnprocessableContent());
        create(admin, cardJson("ZX01", "Admin Fighter", "Rara", 300).replace("Africana", "Atlantidea"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void anAdminEditsAnImportedCardAndTakesItOver() throws Exception {
        String importedCode = jdbcTemplate.queryForObject(
                "SELECT code FROM card WHERE admin_managed = FALSE ORDER BY code LIMIT 1", String.class);

        edit(admin, importedCode, cardJson(importedCode, "Edited Fighter", "Rara", 777))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Edited Fighter")))
                .andExpect(jsonPath("$.atk", is(777)));

        assertFlags(importedCode, true, true);
    }

    @Test
    void editingAnUnknownCardIsNotFound() throws Exception {
        edit(admin, "ZX99", cardJson("ZX99", "Edited Fighter", "Rara", 300)).andExpect(status().isNotFound());
    }

    @Test
    void theCodeOfACardCannotBeChanged() throws Exception {
        create(admin, cardJson("ZX01", "Admin Fighter", "Rara", 300)).andExpect(status().isCreated());

        edit(admin, "ZX01", cardJson("ZX02", "Admin Fighter", "Rara", 300))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    void anEditCanReactivateADeactivatedCard() throws Exception {
        create(admin, cardJson("ZX01", "Admin Fighter", "Rara", 300)).andExpect(status().isCreated());
        jdbcTemplate.update("UPDATE card SET active = FALSE WHERE code = 'ZX01'");
        entityManager.clear();

        edit(admin, "ZX01", cardJson("ZX01", "Admin Fighter", "Rara", 300).replace("}", ", \"active\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)));

        assertFlags("ZX01", true, true);
    }

    @Test
    void aPlayerCannotEditCards() throws Exception {
        edit(player, "ZX01", cardJson("ZX01", "Admin Fighter", "Rara", 300)).andExpect(status().isForbidden());
    }

    private ResultActions edit(String token, String code, String body) throws Exception {
        return mockMvc.perform(put(ADMIN_CARDS + "/" + code).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void aDeactivatedCardLeavesThePublicCatalogButStaysInItsOwnersCollection() throws Exception {
        create(admin, cardJson("ZX01", "Admin Fighter", "Rara", 300)).andExpect(status().isCreated());
        long cardId = jdbcTemplate.queryForObject("SELECT id FROM card WHERE code = 'ZX01'", Long.class);
        long playerId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE nickname = 'acard_player'", Long.class);
        collectionService.grant(playerId, cardId, 1);

        deactivate(admin, "ZX01").andExpect(status().isNoContent());

        assertFlags("ZX01", false, true);
        mockMvc.perform(get("/api/cards").param("faction", "Africana").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", not(hasItem("ZX01"))));
        assertFalse(catalogService.findAllCards().stream().map(CatalogCard::code).anyMatch("ZX01"::equals));
        assertEquals(1, catalogService.findCardsByIds(List.of(cardId)).size());
        mockMvc.perform(get("/api/me/collection").header(HttpHeaders.AUTHORIZATION, player).param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", hasItem("ZX01")));
    }

    @Test
    void anEditCanDeactivateACard() throws Exception {
        create(admin, cardJson("ZX01", "Admin Fighter", "Rara", 300)).andExpect(status().isCreated());

        edit(admin, "ZX01", cardJson("ZX01", "Admin Fighter", "Rara", 300).replace("}", ", \"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void deactivatingAnUnknownCardIsNotFound() throws Exception {
        deactivate(admin, "ZX99").andExpect(status().isNotFound());
    }

    @Test
    void aPlayerCannotDeactivateCards() throws Exception {
        deactivate(player, "ZX01").andExpect(status().isForbidden());
    }

    private ResultActions deactivate(String token, String code) throws Exception {
        return mockMvc.perform(delete(ADMIN_CARDS + "/" + code).header(HttpHeaders.AUTHORIZATION, token));
    }

    private ResultActions create(String token, String body) throws Exception {
        return mockMvc.perform(post(ADMIN_CARDS).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void assertFlags(String code, boolean active, boolean adminManaged) {
        entityManager.flush();
        Map<String, Object> flags = jdbcTemplate.queryForMap(
                "SELECT active, admin_managed FROM card WHERE code = ?", code);
        assertEquals(active, flags.get("active"));
        assertEquals(adminManaged, flags.get("admin_managed"));
    }

    private static String cardJson(String code, String name, String rarity, int attack) {
        return """
                {"code": "%s", "name": "%s", "country": "Kenya", "faction": "Africana",
                 "fighterClass": "Assassini", "rarity": "%s", "atk": %d, "def": 540, "vel": 850, "crt": 400,
                 "flavorText": "Created by an admin", "imageFile": "%s.png"}
                """.formatted(code, name, rarity, attack, code);
    }
}
