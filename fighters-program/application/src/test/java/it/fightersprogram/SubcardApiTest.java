package it.fightersprogram;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import it.fightersprogram.catalog.SubcardService;
import it.fightersprogram.catalog.SubcardView;

@SpringBootTest
@AutoConfigureMockMvc
class SubcardApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubcardService subcardService;

    @Test
    void listsTheSubcardsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/subcards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[0].code", is("SC01")))
                .andExpect(jsonPath("$[0].name", is("Guanti pesanti")))
                .andExpect(jsonPath("$[0].atk", is(12)))
                .andExpect(jsonPath("$[0].def", is(0)))
                .andExpect(jsonPath("$[0].vel", is(-8)))
                .andExpect(jsonPath("$[0].crt", is(0)));
    }

    @Test
    void findsOnlyTheRequestedSubcards() {
        var found = subcardService.findByCodes(List.of("SC02", "SC04"));

        assertEquals(List.of("SC02", "SC04"), found.keySet().stream().sorted().toList());
        assertEquals(-6, found.get("SC04").atk());
    }

    @Test
    void listsTheSubcardsOrderedByCode() {
        List<String> codes = subcardService.findAll().stream().map(SubcardView::code).toList();

        assertEquals(List.of("SC01", "SC02", "SC03", "SC04", "SC05", "SC06",
                "SC07", "SC08", "SC09", "SC10", "SC11", "SC12"), codes);
    }
}
