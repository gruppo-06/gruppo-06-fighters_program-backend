package it.fightersprogram;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.catalog.domain.CardImageStorage;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CardImageApiTest {

    private static final String AF01_IMAGE_URL = "/api/cards/AF01/image";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CardImageStorage storage;

    @BeforeEach
    void restoreTheBundledIllustration() throws Exception {
        storage.save("AF01.webp", new ClassPathResource("catalog/images/AF01.webp").getContentAsByteArray());
    }

    @Test
    void servesTheIllustrationWithoutAToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/cards/AF01/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=86400")))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertEquals("RIFF", new String(body, 0, 4, StandardCharsets.US_ASCII));
        assertEquals("WEBP", new String(body, 8, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void answersNotModifiedWhenTheEtagMatches() throws Exception {
        String etag = mockMvc.perform(get("/api/cards/AF01/image"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(get("/api/cards/AF01/image").header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    void unknownCardHasNoImage() throws Exception {
        mockMvc.perform(get("/api/cards/ZZ99/image")).andExpect(status().isNotFound());
    }

    @Test
    void cardWhoseFileIsMissingHasNoImage() throws Exception {
        jdbcTemplate.update("UPDATE card SET image_file = 'missing-AF01.webp' WHERE code = 'AF01'");

        mockMvc.perform(get("/api/cards/AF01/image")).andExpect(status().isNotFound());
    }

    @Test
    void cardResponseExposesTheImageUrl() throws Exception {
        mockMvc.perform(get("/api/cards/AF01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl", is(AF01_IMAGE_URL)));
    }

    @Test
    void collectionItemsExposeTheImageUrl() throws Exception {
        String token = TestUsers.registerAndLogin(mockMvc, "img_collector");
        long userId = jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE nickname = 'img_collector'",
                Long.class);
        long cardId = jdbcTemplate.queryForObject("SELECT id FROM card WHERE code = 'AF01'", Long.class);
        jdbcTemplate.update("INSERT INTO user_card (user_id, card_id, quantity) VALUES (?, ?, 1)", userId, cardId);

        mockMvc.perform(get("/api/me/collection").param("rarity", "Epica").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code", is("AF01")))
                .andExpect(jsonPath("$.content[*].imageUrl", everyItem(endsWith("/image"))))
                .andExpect(jsonPath("$.content[0].imageUrl", is(AF01_IMAGE_URL)));
    }
}
