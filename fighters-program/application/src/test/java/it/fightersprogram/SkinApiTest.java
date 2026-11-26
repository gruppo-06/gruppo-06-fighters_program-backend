package it.fightersprogram;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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
class SkinApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listsActiveSkinsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/skins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(10)))
                .andExpect(jsonPath("$.content[*].code", hasItem("SK_AV_001")))
                .andExpect(jsonPath("$.content[*].code", hasItem("SK_CV_001")))
                .andExpect(jsonPath("$.content[0].code", is("SK_AV_001")));
    }

    @Test
    void filtersByTypeAndCardCodeTogether() throws Exception {
        mockMvc.perform(get("/api/skins").param("type", "CARD_VARIANT").param("cardCode", "EA01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].code", is("SK_CV_001")))
                .andExpect(jsonPath("$.content[0].cardCode", is("EA01")));
    }

    @Test
    void paginatesTheSkinCatalog() throws Exception {
        mockMvc.perform(get("/api/skins").param("page", "1").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.size", is(3)))
                .andExpect(jsonPath("$.totalElements", is(10)))
                .andExpect(jsonPath("$.totalPages", is(4)))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].code", is("SK_AV_004")));
    }

    @Test
    void filtersByCardCodeWithoutType() throws Exception {
        mockMvc.perform(get("/api/skins").param("cardCode", "EA01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].type", is("CARD_VARIANT")));
    }

    @Test
    void filtersByTypeWithoutCardCodeAndReturnsNullCardCodeForAvatars() throws Exception {
        mockMvc.perform(get("/api/skins").param("type", "AVATAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(4)))
                .andExpect(jsonPath("$.content[0].code", is("SK_AV_001")))
                .andExpect(jsonPath("$.content[0].cardCode", nullValue()))
                .andExpect(jsonPath("$.content[0].rarity", is("Rara")))
                .andExpect(jsonPath("$.content[0].price", is(250)))
                .andExpect(jsonPath("$.content[0].imageFile", is("avatars/neon-vanguard.png")));
    }

    @Test
    void combinedFiltersReturnAnEmptyPageWhenTheyDoNotMatch() throws Exception {
        mockMvc.perform(get("/api/skins").param("type", "AVATAR").param("cardCode", "EA01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void doesNotListInactiveSkins() throws Exception {
        jdbcTemplate.update("UPDATE skin SET active = FALSE WHERE code = 'SK_AV_001'");

        mockMvc.perform(get("/api/skins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(9)))
                .andExpect(jsonPath("$.content[*].code", not(hasItem("SK_AV_001"))));
    }

    @Test
    void getsSkinDetailsAndReturnsNotFoundForUnknownCode() throws Exception {
        mockMvc.perform(get("/api/skins/SK_CV_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code", is("SK_CV_001")))
                .andExpect(jsonPath("$.name", is("Daichi Nightfall")))
                .andExpect(jsonPath("$.type", is("CARD_VARIANT")))
                .andExpect(jsonPath("$.cardCode", is("EA01")))
                .andExpect(jsonPath("$.rarity", is("Rara")))
                .andExpect(jsonPath("$.price", is(300)))
                .andExpect(jsonPath("$.imageFile", is("variants/ea01-nightfall.png")));

        mockMvc.perform(get("/api/skins/NO-SUCH-SKIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }
}
