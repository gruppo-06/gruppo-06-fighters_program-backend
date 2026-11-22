package it.fightersprogram.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import it.fightersprogram.common.error.ApiError;

/**
 * Makes the published contract exact, so the frontend can generate its types from it: Jackson writes every record
 * component, null included, hence every property is required and only the ones listed here may be null.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";
    private static final String NULL_TYPE = "null";

    private static final Map<String, Set<String>> NULLABLE_PROPERTIES = Map.of(
            "CardResponse", Set.of("country", "flavorText", "imageFile", "imageUrl"),
            "CollectionEntry", Set.of("imageFile", "imageUrl"),
            "DeckCardView", Set.of("imageFile"),
            "MatchReplay", Set.of("winnerNickname", "finishedAt", "challengerDeck", "opponentDeck", "log"),
            "TurnLog", Set.of("replacementOne", "replacementTwo"));

    @Bean
    OpenAPI fightersProgramOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Fighters Program API").version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    OpenApiCustomizer exactContractCustomizer() {
        return openApi -> {
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            schemas.putAll(ModelConverters.getInstance(true).read(ApiError.class));
            schemas.values().forEach(OpenApiConfig::requireEveryProperty);
            NULLABLE_PROPERTIES.forEach((schemaName, properties) ->
                    properties.forEach(property -> allowNull(schemas, schemaName, property)));
        };
    }

    private static void requireEveryProperty(Schema<?> schema) {
        if (schema.getProperties() != null) {
            schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
        }
    }

    private static void allowNull(Map<String, Schema> schemas, String schemaName, String propertyName) {
        Schema<?> schema = schemas.get(schemaName);
        if (schema == null || schema.getProperties() == null || !schema.getProperties().containsKey(propertyName)) {
            throw new IllegalStateException("Nullable property not found in the API docs: " + schemaName + "."
                    + propertyName);
        }
        Schema<?> property = schema.getProperties().get(propertyName);
        if (property.get$ref() != null) {
            schema.getProperties().put(propertyName, new Schema<>().anyOf(List.of(property, nullSchema())));
        } else {
            property.addType(NULL_TYPE);
        }
    }

    private static Schema<?> nullSchema() {
        Schema<?> schema = new Schema<>();
        schema.addType(NULL_TYPE);
        return schema;
    }
}
