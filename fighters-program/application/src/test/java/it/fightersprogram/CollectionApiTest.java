package it.fightersprogram;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

class CollectionApiTest {

    private static final int FACTIONS = 5;

    @Nested
    @SpringBootTest(properties = "game.starter-collection.enabled=false")
    @AutoConfigureMockMvc
    @Transactional
    class WithoutStarterCollection {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Test
        void aNewUserHasAnEmptyCollection() throws Exception {
            String token = TestUsers.registerAndLogin(mockMvc, "col_empty");

            mockMvc.perform(get("/api/me/collection").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements", is(0)))
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        void aNewUserHasZeroProgressInEveryFaction() throws Exception {
            String token = TestUsers.registerAndLogin(mockMvc, "col_empty");
            int euroAtlanticCards = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM card c JOIN faction f ON f.id = c.faction_id WHERE f.name = 'Euroatlantica'",
                    Integer.class);

            mockMvc.perform(get("/api/me/collection/progress").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(FACTIONS)))
                    .andExpect(jsonPath("$[*].owned", everyItem(is(0))))
                    .andExpect(jsonPath("$[?(@.faction == 'Euroatlantica')].total", everyItem(is(euroAtlanticCards))));
        }

        @Test
        void requiresAToken() throws Exception {
            mockMvc.perform(get("/api/me/collection")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @SpringBootTest(properties = "game.starter-collection.enabled=true")
    @AutoConfigureMockMvc
    @Transactional
    class WithStarterCollection {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Test
        void aNewUserGetsTwoCopiesOfEveryCommonAndOneOfEveryRare() throws Exception {
            String token = TestUsers.registerAndLogin(mockMvc, "col_starter");

            mockMvc.perform(get("/api/me/collection").param("rarity", "Comune").param("size", "100")
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements", is(cardsOfRarity("Comune"))))
                    .andExpect(jsonPath("$.content[*].quantity", everyItem(is(2))));
            mockMvc.perform(get("/api/me/collection").param("rarity", "Rara").param("size", "100")
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(jsonPath("$.totalElements", is(cardsOfRarity("Rara"))))
                    .andExpect(jsonPath("$.content[*].quantity", everyItem(is(1))));
            mockMvc.perform(get("/api/me/collection").param("rarity", "Leggendaria")
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }

        @Test
        void filtersByFactionAndPaginatesByCode() throws Exception {
            String token = TestUsers.registerAndLogin(mockMvc, "col_starter");

            mockMvc.perform(get("/api/me/collection").param("faction", "Euroatlantica").param("size", "3")
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.content[0].code", is(firstStarterCode("Euroatlantica"))))
                    .andExpect(jsonPath("$.content[*].faction", everyItem(is("Euroatlantica"))));
        }

        @Test
        void eachUserSeesOnlyTheirOwnCollection() throws Exception {
            String alice = TestUsers.registerAndLogin(mockMvc, "col_alice");
            TestUsers.registerAndLogin(mockMvc, "col_bob");
            long aliceId = jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE nickname = 'col_alice'",
                    Long.class);
            long legendaryId = TestDecks.legendaryCardId(jdbcTemplate, "Euroatlantica");
            jdbcTemplate.update("INSERT INTO user_card (user_id, card_id, quantity) VALUES (?, ?, 1)",
                    aliceId, legendaryId);

            mockMvc.perform(get("/api/me/collection").param("rarity", "Leggendaria")
                            .header(HttpHeaders.AUTHORIZATION, alice))
                    .andExpect(jsonPath("$.totalElements", is(1)));
            mockMvc.perform(get("/api/me/collection/progress").header(HttpHeaders.AUTHORIZATION, alice))
                    .andExpect(jsonPath("$[?(@.faction == 'Euroatlantica')].owned",
                            everyItem(is(cardsOfFactionAndRarity("Euroatlantica") + 1))));
        }

        private String firstStarterCode(String faction) {
            return jdbcTemplate.queryForObject("""
                    SELECT min(c.code) FROM card c
                    JOIN rarity r ON r.id = c.rarity_id JOIN faction f ON f.id = c.faction_id
                    WHERE f.name = ? AND r.name IN ('Comune', 'Rara')
                    """, String.class, faction);
        }

        private int cardsOfRarity(String rarity) {
            return jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM card c JOIN rarity r ON r.id = c.rarity_id WHERE r.name = ?",
                    Integer.class, rarity);
        }

        private int cardsOfFactionAndRarity(String faction) {
            return jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM card c
                    JOIN rarity r ON r.id = c.rarity_id JOIN faction f ON f.id = c.faction_id
                    WHERE f.name = ? AND r.name IN ('Comune', 'Rara')
                    """, Integer.class, faction);
        }
    }
}
