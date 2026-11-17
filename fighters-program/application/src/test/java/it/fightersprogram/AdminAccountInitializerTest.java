package it.fightersprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.Filter;

class AdminAccountInitializerTest {

    @Test
    void failsStartupWhenAdminPasswordIsShorterThan12Characters() {
        assertThrows(Exception.class, () -> {
            try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(FightersProgramApplication.class)
                    .properties(
                            "server.port=0",
                            "identity.admin.email=shortpass@example.com",
                            "identity.admin.password=too-short",
                            "identity.admin.nickname=shortadmin")
                    .run()) {
                // startup must fail before reaching here
            }
        });
    }

    @Test
    void seedsAdminOnceAndIssuesJwtWithAdminRoleWhenConfigured() throws Exception {
        String email = "seed_admin@example.com";
        String password = "correct-horse-battery-staple";
        String nickname = "seed_admin";

        try {
            // 1. First startup: creates the admin user with role ADMIN and no starter cards
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(FightersProgramApplication.class)
                    .properties(
                            "server.port=0",
                            "identity.admin.email=" + email,
                            "identity.admin.password=" + password,
                            "identity.admin.nickname=" + nickname)
                    .run()) {
                JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
                Map<String, Object> user = jdbc.queryForMap(
                        "SELECT id, nickname, role FROM app_user WHERE email = ?", email);
                assertEquals(nickname, user.get("nickname"));
                assertEquals("ADMIN", user.get("role"));

                Long userId = ((Number) user.get("id")).longValue();
                Integer cardsCount = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM user_card WHERE user_id = ?", Integer.class, userId);
                assertEquals(0, cardsCount);
            }

            // 2. Second startup (restart): does not create a duplicate admin and issues JWT with roles: ["ADMIN"]
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(FightersProgramApplication.class)
                    .properties(
                            "server.port=0",
                            "identity.admin.email=" + email,
                            "identity.admin.password=" + password,
                            "identity.admin.nickname=" + nickname)
                    .run()) {
                JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
                Integer adminCount = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM app_user WHERE email = ?", Integer.class, email);
                assertEquals(1, adminCount);

                Filter springSecurityFilterChain = context.getBean("springSecurityFilterChain", Filter.class);
                MockMvc mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
                        .addFilters(springSecurityFilterChain)
                        .build();

                String loginResponse = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email": "%s", "password": "%s"}
                                        """.formatted(email, password)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").isString())
                        .andReturn().getResponse().getContentAsString();

                String token = JsonPath.read(loginResponse, "$.accessToken");
                String payloadJson = new String(
                        Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
                List<String> roles = JsonPath.read(payloadJson, "$.roles");
                assertEquals(List.of("ADMIN"), roles);
            }
        } finally {
            try (ConfigurableApplicationContext cleanup = new SpringApplicationBuilder(FightersProgramApplication.class)
                    .properties("server.port=0")
                    .run()) {
                cleanup.getBean(JdbcTemplate.class).update("DELETE FROM app_user WHERE email = ?", email);
            }
        }
    }

    @Test
    void doesNotModifyExistingUserWithSameEmail() {
        String email = "existing_player@example.com";
        String nickname = "existing_player";

        try {
            try (ConfigurableApplicationContext setupContext = new SpringApplicationBuilder(FightersProgramApplication.class)
                    .properties("server.port=0")
                    .run()) {
                setupContext.getBean(JdbcTemplate.class).update(
                        "INSERT INTO app_user (nickname, email, password_hash, role) VALUES (?, ?, ?, 'PLAYER')",
                        nickname, email, "original-hash");
            }

            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(FightersProgramApplication.class)
                    .properties(
                            "server.port=0",
                            "identity.admin.email=" + email,
                            "identity.admin.password=valid-long-password-123",
                            "identity.admin.nickname=" + nickname)
                    .run()) {
                JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
                Map<String, Object> user = jdbc.queryForMap(
                        "SELECT role, password_hash FROM app_user WHERE email = ?", email);
                assertEquals("PLAYER", user.get("role"));
                assertEquals("original-hash", user.get("password_hash"));
            }
        } finally {
            try (ConfigurableApplicationContext cleanup = new SpringApplicationBuilder(FightersProgramApplication.class)
                    .properties("server.port=0")
                    .run()) {
                cleanup.getBean(JdbcTemplate.class).update("DELETE FROM app_user WHERE email = ?", email);
            }
        }
    }
}