package it.fightersprogram;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.collection.CollectionService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SkinApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CollectionService collectionService;

    @Test
    void playerCanBrowsePurchaseAndActivateSkinsVisibleInAnotherPlayersAppearance() throws Exception {
        String ownerNickname = nickname("owner");
        String viewerNickname = nickname("viewer");
        String ownerToken = TestUsers.registerAndLogin(mockMvc, ownerNickname);
        String viewerToken = TestUsers.registerAndLogin(mockMvc, viewerNickname);
        long avatarId = skinId("SK_AV_003");
        long variantId = skinId("SK_CV_001");
        long cardId = cardId("EA01");
        long ownerId = userId(ownerNickname);
        collectionService.grant(ownerId, cardId, 1);

        mockMvc.perform(get("/api/skins").param("type", "AVATAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.code == 'SK_AV_003')]", hasSize(1)));
        mockMvc.perform(get("/api/skins/SK_CV_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardCode", is("EA01")));

        mockMvc.perform(post("/api/skins/" + avatarId + "/purchase")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/skins/" + variantId + "/purchase")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isCreated());

        putSkin("/api/me/skins/avatar", ownerToken, avatarId)
                .andExpect(status().isNoContent());
        putSkin("/api/me/skins/cards/" + cardId, ownerToken, variantId)
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/players/" + ownerNickname + "/appearance")
                        .header(HttpHeaders.AUTHORIZATION, viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.code", is("SK_AV_003")))
                .andExpect(jsonPath("$.cardVariants", hasSize(1)))
                .andExpect(jsonPath("$.cardVariants[0].cardId", is((int) cardId)))
                .andExpect(jsonPath("$.cardVariants[0].skin.code", is("SK_CV_001")));
    }

    @Test
    void meSkinEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/me/skins")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/me/skins/avatar")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"skinId\": 1}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/me/skins/cards/1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"skinId\": 1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingSkinsReturnNotFoundAndDuplicatePurchaseConflicts() throws Exception {
        String token = TestUsers.registerAndLogin(mockMvc, nickname("missing"));
        mockMvc.perform(get("/api/skins/NO_SUCH_SKIN")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/skins/999999/purchase")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());

        long avatarId = skinId("SK_AV_003");
        mockMvc.perform(post("/api/skins/" + avatarId + "/purchase")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/skins/" + avatarId + "/purchase")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidSkinSelectionsReturnUnprocessableEntity() throws Exception {
        String nickname = nickname("invalid");
        String token = TestUsers.registerAndLogin(mockMvc, nickname);
        long avatarId = skinId("SK_AV_003");
        long variantId = skinId("SK_CV_001");
        long wrongCardVariantId = skinId("SK_CV_002");
        long cardId = cardId("EA01");

        mockMvc.perform(put("/api/me/skins/avatar").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"skinId\": " + avatarId + "}"))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(put("/api/me/skins/cards/" + cardId).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"skinId\": " + variantId + "}"))
                .andExpect(status().isUnprocessableContent());

        long userId = userId(nickname);
        jdbcTemplate.update("INSERT INTO user_skin (user_id, skin_id) VALUES (?, ?)", userId, avatarId);
        jdbcTemplate.update("INSERT INTO user_skin (user_id, skin_id) VALUES (?, ?)", userId, variantId);
        jdbcTemplate.update("INSERT INTO user_skin (user_id, skin_id) VALUES (?, ?)", userId, wrongCardVariantId);
        mockMvc.perform(put("/api/me/skins/avatar").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"skinId\": " + variantId + "}"))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(put("/api/me/skins/cards/" + cardId).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"skinId\": " + wrongCardVariantId + "}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void swaggerDocumentsSkinEndpointsInTheSkinsGroup() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/skins'].get.tags[0]", is("Skins")))
                .andExpect(jsonPath("$.paths['/api/skins/{code}'].get.tags[0]", is("Skins")))
                .andExpect(jsonPath("$.paths['/api/skins/{id}/purchase'].post.tags[0]", is("Skins")))
                .andExpect(jsonPath("$.paths['/api/me/skins'].get.tags[0]", is("Skins")))
                .andExpect(jsonPath("$.paths['/api/me/skins/avatar'].put.tags[0]", is("Skins")))
                .andExpect(jsonPath("$.paths['/api/me/skins/cards/{cardId}'].put.tags[0]", is("Skins")))
                .andExpect(jsonPath("$.paths['/api/players/{nickname}/appearance'].get.tags[0]", is("Skins")));
    }

    private org.springframework.test.web.servlet.ResultActions putSkin(String path, String token, long skinId)
            throws Exception {
        return mockMvc.perform(put(path)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skinId\": " + skinId + "}"));
    }

    private long userId(String nickname) {
        return jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE nickname = ?", Long.class, nickname);
    }

    private long skinId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM skin WHERE code = ?", Long.class, code);
    }

    private long cardId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM card WHERE code = ?", Long.class, code);
    }

    private static String nickname(String prefix) {
        return "sk" + prefix.substring(0, Math.min(prefix.length(), 4))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
