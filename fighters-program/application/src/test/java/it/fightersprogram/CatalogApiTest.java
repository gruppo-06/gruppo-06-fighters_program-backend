package it.fightersprogram;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.importer.CardEntry;
import it.fightersprogram.catalog.importer.CardImage;
import it.fightersprogram.catalog.importer.CardStats;
import it.fightersprogram.catalog.importer.CatalogFile;
import it.fightersprogram.catalog.importer.CatalogImporter;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogApiTest {

    private static final String FACTION = "Africana";
    private static final int CARDS_PER_FACTION = 16;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CatalogImporter importer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int africanCardsBefore;

    @BeforeEach
    void importTestFaction() {
        africanCardsBefore = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM card c JOIN faction f ON f.id = c.faction_id WHERE f.name = ?",
                Integer.class, FACTION);
        importer.importAll(List.of(testFaction()));
    }

    @Test
    void listsEveryCardOfAFaction() throws Exception {
        mockMvc.perform(get("/api/cards").param("faction", FACTION).param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(africanCardsBefore + CARDS_PER_FACTION)))
                .andExpect(jsonPath("$.content[*].code", hasItems("ZA01", "ZA08", "ZA09", "ZA16")))
                .andExpect(jsonPath("$.content[*].faction", everyItem(is(FACTION))));
    }

    @Test
    void filtersByRarity() throws Exception {
        mockMvc.perform(get("/api/cards").param("faction", FACTION).param("rarity", "Leggendaria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", hasItem("ZA16")))
                .andExpect(jsonPath("$.content[*].rarity", everyItem(is("Leggendaria"))));
    }

    @Test
    void filtersByClass() throws Exception {
        mockMvc.perform(get("/api/cards").param("faction", FACTION).param("class", "Colossi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", hasItem("ZA04")))
                .andExpect(jsonPath("$.content[*].fighterClass", everyItem(is("Colossi"))));
    }

    @Test
    void paginatesTheResults() throws Exception {
        mockMvc.perform(get("/api/cards").param("faction", FACTION).param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(5)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.page", is(0)));
    }

    @Test
    void returnsEveryFieldOfACard() throws Exception {
        mockMvc.perform(get("/api/cards/ZA16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("ZA16")))
                .andExpect(jsonPath("$.name", is("Fighter ZA16")))
                .andExpect(jsonPath("$.country", is("Kenya")))
                .andExpect(jsonPath("$.faction", is(FACTION)))
                .andExpect(jsonPath("$.rarity", is("Leggendaria")))
                .andExpect(jsonPath("$.atk", is(281)))
                .andExpect(jsonPath("$.def", is(540)))
                .andExpect(jsonPath("$.vel", is(850)))
                .andExpect(jsonPath("$.crt", is(400)))
                .andExpect(jsonPath("$.flavorText", is("Flavor ZA16")))
                .andExpect(jsonPath("$.imageFile", is("ZA16.png")));
    }

    @Test
    void unknownCardIsNotFound() throws Exception {
        mockMvc.perform(get("/api/cards/ZZ00"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Card not found: ZZ00")));
    }

    private static CatalogFile testFaction() {
        List<String> classes = List.of("Combattenti", "Assassini", "Specialisti", "Colossi", "Sentinelle");
        List<CardEntry> cards = new ArrayList<>();
        for (int number = 1; number <= CARDS_PER_FACTION; number++) {
            String code = "ZA%02d".formatted(number);
            cards.add(new CardEntry(code, "Fighter " + code, "Kenya", classes.get((number - 1) % classes.size()),
                    rarityOf(number), new CardStats(281, 540, 850, 400), "Flavor " + code,
                    new CardImage(code + ".png", "image/png", 900, 1200)));
        }
        return new CatalogFile(1, FACTION, cards);
    }

    private static String rarityOf(int number) {
        if (number <= 8) {
            return "Comune";
        }
        if (number <= 12) {
            return "Rara";
        }
        return number <= 15 ? "Epica" : "Leggendaria";
    }
}
