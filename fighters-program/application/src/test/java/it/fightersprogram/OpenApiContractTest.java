package it.fightersprogram;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {

    private static final String SCHEMAS = "$.components.schemas.";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void everyPropertyOfAResponseIsRequiredBecauseRecordsAlwaysSerialiseThem() throws Exception {
        apiDocs()
                .andExpect(jsonPath(SCHEMAS + "CardResponse.required", hasItems("code", "name", "atk", "country")))
                .andExpect(jsonPath(SCHEMAS + "MatchReplay.required", hasItems("id", "status", "log")))
                .andExpect(jsonPath(SCHEMAS + "TurnLog.required", hasItems("turnNumber", "replacementOne")));
    }

    @Test
    void propertiesThatCanBeNullAcceptNull() throws Exception {
        apiDocs()
                .andExpect(jsonPath(SCHEMAS + "MatchReplay.properties.winnerNickname.type", hasItem("null")))
                .andExpect(jsonPath(SCHEMAS + "MatchReplay.properties.finishedAt.type", hasItem("null")))
                .andExpect(jsonPath(SCHEMAS + "MatchReplay.properties.challengerDeck.type", hasItem("null")))
                .andExpect(jsonPath(SCHEMAS + "MatchReplay.properties.log.anyOf[*].type", hasItem("null")))
                .andExpect(jsonPath(SCHEMAS + "TurnLog.properties.replacementOne.anyOf[*].type", hasItem("null")))
                .andExpect(jsonPath(SCHEMAS + "CardResponse.properties.imageFile.type", hasItem("null")));
    }

    @Test
    void propertiesThatAreNeverNullDoNotAcceptNull() throws Exception {
        apiDocs()
                .andExpect(jsonPath(SCHEMAS + "MatchReplay.properties.status.type", not(hasItem("null"))))
                .andExpect(jsonPath(SCHEMAS + "CardResponse.properties.code.type", not(hasItem("null"))));
    }

    @Test
    void theErrorFormatIsPublished() throws Exception {
        apiDocs().andExpect(jsonPath(SCHEMAS + "ApiError.required", containsInAnyOrder("code", "message", "details")));
    }

    @Test
    void theBearerTokenSchemeIsPublished() throws Exception {
        apiDocs()
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type", is("http")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme", is("bearer")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat", is("JWT")));
    }

    private ResultActions apiDocs() throws Exception {
        return mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
