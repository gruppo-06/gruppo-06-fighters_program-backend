package it.fightersprogram;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import it.fightersprogram.identity.UserRegistered;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class IdentityApiTest {

    private static final String PASSWORD = "correct-horse-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationEvents events;

    @Test
    void registeringCreatesAPlayerWithoutExposingThePassword() throws Exception {
        register("id_alice", "id_alice@example.com", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", greaterThan(0)))
                .andExpect(jsonPath("$.nickname", is("id_alice")))
                .andExpect(jsonPath("$.email", is("id_alice@example.com")))
                .andExpect(jsonPath("$.role", is("PLAYER")))
                .andExpect(content().string(not(containsString(PASSWORD))))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void storesOnlyAHashOfThePassword() throws Exception {
        register("id_alice", "id_alice@example.com", PASSWORD).andExpect(status().isCreated());

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM app_user WHERE email = 'id_alice@example.com'", String.class);
        assertNotEquals(PASSWORD, storedHash);
        assertThat(storedHash, startsWith("$2"));
    }

    @Test
    void announcesTheNewUserToTheOtherModules() throws Exception {
        String body = register("id_alice", "id_alice@example.com", PASSWORD).andReturn().getResponse()
                .getContentAsString();
        long userId = ((Number) JsonPath.read(body, "$.id")).longValue();

        assertEquals(1, events.stream(UserRegistered.class).filter(event -> event.userId() == userId).count());
    }

    @Test
    void rejectsADuplicateEmail() throws Exception {
        register("id_alice", "id_alice@example.com", PASSWORD).andExpect(status().isCreated());

        register("id_other", "id_alice@example.com", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));
    }

    @Test
    void rejectsADuplicateNickname() throws Exception {
        register("id_alice", "id_alice@example.com", PASSWORD).andExpect(status().isCreated());

        register("id_alice", "id_other@example.com", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));
    }

    @Test
    void rejectsAShortPassword() throws Exception {
        register("id_alice", "id_alice@example.com", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.details[0]", startsWith("password")));
    }

    @Test
    void rejectsAnInvalidEmailAndATooShortNickname() throws Exception {
        register("ab", "not-an-email", PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()", is(2)));
    }

    @Test
    void rejectsAMalformedBody() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{ \"nick"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MALFORMED_REQUEST")));
    }

    @Test
    void loggingInReturnsABearerToken() throws Exception {
        register("id_alice", "id_alice@example.com", PASSWORD).andExpect(status().isCreated());

        login("id_alice@example.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.expiresIn", is(3600)));
    }

    @Test
    void aWrongPasswordAndAnUnknownEmailGetTheSameGenericAnswer() throws Exception {
        register("id_alice", "id_alice@example.com", PASSWORD).andExpect(status().isCreated());

        String wrongPassword = login("id_alice@example.com", "wrong-password").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = login("nobody@example.com", PASSWORD).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertEquals(wrongPassword, unknownEmail);
    }

    @Test
    void returnsTheProfileOfTheCurrentUser() throws Exception {
        String token = registerAndLogin("id_alice", "id_alice@example.com");

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname", is("id_alice")))
                .andExpect(jsonPath("$.email", is("id_alice@example.com")))
                .andExpect(jsonPath("$.role", is("PLAYER")));
    }

    @Test
    void aProtectedEndpointWithoutATokenAnswers401InTheCommonFormat() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void aForgedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aPlayerCannotReachTheAdminApi() throws Exception {
        String token = registerAndLogin("id_alice", "id_alice@example.com");

        mockMvc.perform(get("/api/admin/anything").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    private String registerAndLogin(String nickname, String email) throws Exception {
        register(nickname, email, PASSWORD).andExpect(status().isCreated());
        String body = login(email, PASSWORD).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private ResultActions register(String nickname, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"nickname": "%s", "email": "%s", "password": "%s"}
                """.formatted(nickname, email, password)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email": "%s", "password": "%s"}
                """.formatted(email, password)));
    }
}
