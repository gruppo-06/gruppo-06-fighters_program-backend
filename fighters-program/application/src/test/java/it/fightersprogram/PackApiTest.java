package it.fightersprogram;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PackApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listsActivePacksWithoutToken() throws Exception {
        mockMvc.perform(get("/api/packs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(3)))
                .andExpect(jsonPath("$[0].name", is("Pacchetto Base")))
                .andExpect(jsonPath("$[0].price", is(100)))
                .andExpect(jsonPath("$[0].totalCards", is(5)))
                .andExpect(jsonPath("$[0].contents[0].rarity", is("Comune")))
                .andExpect(jsonPath("$[0].contents[0].cardCount", is(4)))
                .andExpect(jsonPath("$[0].contents[1].rarity", is("Rara")))
                .andExpect(jsonPath("$[0].contents[1].cardCount", is(1)))
                .andExpect(jsonPath("$[1].name", is("Pacchetto Avanzato")))
                .andExpect(jsonPath("$[1].price", is(220)))
                .andExpect(jsonPath("$[1].totalCards", is(8)))
                .andExpect(jsonPath("$[2].name", is("Pacchetto Leggendario")))
                .andExpect(jsonPath("$[2].price", is(500)))
                .andExpect(jsonPath("$[2].totalCards", is(10)));
    }

    @Test
    void packDetailWithoutToken() throws Exception {
        long basePackId = jdbcTemplate.queryForObject(
                "SELECT id FROM pack WHERE name = 'Pacchetto Base'", Long.class);

        mockMvc.perform(get("/api/packs/" + basePackId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) basePackId)))
                .andExpect(jsonPath("$.name", is("Pacchetto Base")))
                .andExpect(jsonPath("$.price", is(100)))
                .andExpect(jsonPath("$.totalCards", is(5)))
                .andExpect(jsonPath("$.contents.length()", is(2)))
                .andExpect(jsonPath("$.contents[0].rarity", is("Comune")))
                .andExpect(jsonPath("$.contents[0].cardCount", is(4)))
                .andExpect(jsonPath("$.contents[1].rarity", is("Rara")))
                .andExpect(jsonPath("$.contents[1].cardCount", is(1)));
    }

    @Test
    void unknownPackReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/packs/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    void inactivePackReturnsNotFound() throws Exception {
        long basePackId = jdbcTemplate.queryForObject(
                "SELECT id FROM pack WHERE name = 'Pacchetto Base'", Long.class);

        jdbcTemplate.update("UPDATE pack SET active = false WHERE id = ?", basePackId);

        mockMvc.perform(get("/api/packs/" + basePackId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));

        mockMvc.perform(get("/api/packs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[*].name", hasItem("Pacchetto Avanzato")))
                .andExpect(jsonPath("$[*].name", hasItem("Pacchetto Leggendario")));
    }
}