package it.fightersprogram;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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

import it.fightersprogram.collection.CardTransfer;
import it.fightersprogram.collection.CollectionService;
import it.fightersprogram.cosmetics.SkinService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SkinAppearanceApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SkinService skinService;

    @Autowired
    private CollectionService collectionService;

    @Test
    void userCanSelectAndResetAnOwnedAvatarAndCardVariant() throws Exception {
        String nickname = nickname("appearance");
        String token = TestUsers.registerAndLogin(mockMvc, nickname);
        long userId = userId(nickname);
        long cardId = cardId("EA01");
        long avatarId = skinId("SK_AV_001");
        long variantId = skinId("SK_CV_001");
        skinService.grant(userId, avatarId, "TEST");
        skinService.grant(userId, variantId, "TEST");
        collectionService.grant(userId, cardId, 1);

        putJson("/api/me/skins/avatar", token, "{\"skinId\": %d}".formatted(avatarId))
                .andExpect(status().isNoContent());
        putJson("/api/me/skins/cards/" + cardId, token, "{\"skinId\": %d}".formatted(variantId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/players/" + nickname + "/appearance")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.code", is("SK_AV_001")))
                .andExpect(jsonPath("$.cardVariants", hasSize(1)))
                .andExpect(jsonPath("$.cardVariants[0].cardId", is((int) cardId)))
                .andExpect(jsonPath("$.cardVariants[0].skin.code", is("SK_CV_001")));

        putJson("/api/me/skins/avatar", token, "{\"skinId\": null}").andExpect(status().isNoContent());
        putJson("/api/me/skins/cards/" + cardId, token, "{\"skinId\": null}")
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/players/" + nickname + "/appearance")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar", nullValue()))
                .andExpect(jsonPath("$.cardVariants", hasSize(0)));
    }

    @Test
    void cannotSelectAnUnownedSkinOrASkinOfTheWrongTypeOrCard() throws Exception {
        String nickname = nickname("invalid");
        String token = TestUsers.registerAndLogin(mockMvc, nickname);
        long userId = userId(nickname);
        long cardId = cardId("EA01");
        long avatarId = skinId("SK_AV_001");
        long variantId = skinId("SK_CV_001");
        long otherCardVariantId = skinId("SK_CV_002");
        skinService.grant(userId, avatarId, "TEST");
        skinService.grant(userId, variantId, "TEST");
        skinService.grant(userId, otherCardVariantId, "TEST");
        collectionService.grant(userId, cardId, 1);

        putJson("/api/me/skins/avatar", token, "{\"skinId\": %d}".formatted(variantId))
                .andExpect(status().isUnprocessableContent());
        putJson("/api/me/skins/avatar", token, "{\"skinId\": 999999}")
                .andExpect(status().isUnprocessableContent());
        putJson("/api/me/skins/cards/" + cardId, token, "{\"skinId\": %d}".formatted(avatarId))
                .andExpect(status().isUnprocessableContent());
        putJson("/api/me/skins/cards/" + cardId, token, "{\"skinId\": %d}".formatted(otherCardVariantId))
                .andExpect(status().isUnprocessableContent());
        putJson("/api/me/skins/cards/" + cardId, token, "{\"skinId\": 999999}")
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void cardVariantCannotBeSelectedWithoutOwningItsCard() throws Exception {
        String nickname = nickname("nocard");
        String token = TestUsers.registerAndLogin(mockMvc, nickname);
        long userId = userId(nickname);
        long cardId = cardId("EA01");
        long variantId = skinId("SK_CV_001");
        skinService.grant(userId, variantId, "TEST");
        jdbcTemplate.update("DELETE FROM user_card WHERE user_id = ? AND card_id = ?", userId, cardId);
        assertFalse(collectionService.ownedQuantities(userId).containsKey(cardId));

        putJson("/api/me/skins/cards/" + cardId, token, "{\"skinId\": %d}".formatted(variantId))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void appearanceHidesSavedVariantAfterTheCardIsTransferredAway() throws Exception {
        String ownerNickname = nickname("owner");
        String viewerNickname = nickname("viewer");
        String ownerToken = TestUsers.registerAndLogin(mockMvc, ownerNickname);
        String viewerToken = TestUsers.registerAndLogin(mockMvc, viewerNickname);
        long ownerId = userId(ownerNickname);
        long viewerId = userId(viewerNickname);
        long cardId = cardId("EA01");
        long variantId = skinId("SK_CV_001");
        skinService.grant(ownerId, variantId, "TEST");
        int copies = collectionService.ownedQuantities(ownerId).getOrDefault(cardId, 0);
        if (copies == 0) {
            collectionService.grant(ownerId, cardId, 1);
            copies = 1;
        }
        putJson("/api/me/skins/cards/" + cardId, ownerToken, "{\"skinId\": %d}".formatted(variantId))
                .andExpect(status().isNoContent());

        collectionService.transfer(List.of(new CardTransfer(ownerId, viewerId, cardId, copies)));

        mockMvc.perform(get("/api/players/" + ownerNickname + "/appearance")
                        .header(HttpHeaders.AUTHORIZATION, viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardVariants", hasSize(0)));
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT count(*) FROM user_card_variant WHERE user_id = ? AND card_id = ?
                """, Integer.class, ownerId, cardId));
    }

    @Test
    void appearanceRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/players/someone/appearance")).andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions putJson(String path, String token, String json)
            throws Exception {
        return mockMvc.perform(put(path)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private long userId(String nickname) {
        return jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE nickname = ?", Long.class, nickname);
    }

    private long cardId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM card WHERE code = ?", Long.class, code);
    }

    private long skinId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM skin WHERE code = ?", Long.class, code);
    }

    private static String nickname(String prefix) {
        return "sa" + prefix.substring(0, Math.min(prefix.length(), 4))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
