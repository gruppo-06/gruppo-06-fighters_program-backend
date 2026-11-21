package it.fightersprogram;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
