package it.fightersprogram;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

final class TestUsers {

    private static final String PASSWORD = "correct-horse-1";

    private TestUsers() {
    }

    static String registerAndLogin(MockMvc mockMvc, String nickname) throws Exception {
        register(mockMvc, nickname);
        return login(mockMvc, nickname);
    }

    static String registerAndLoginAsAdmin(MockMvc mockMvc, EntityManager entityManager, String nickname)
            throws Exception {
        register(mockMvc, nickname);
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE app_user SET role = 'ADMIN' WHERE nickname = :nickname")
                .setParameter("nickname", nickname)
                .executeUpdate();
        entityManager.clear();
        return login(mockMvc, nickname);
    }

    private static void register(MockMvc mockMvc, String nickname) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"nickname": "%s", "email": "%s", "password": "%s"}
                """.formatted(nickname, email(nickname), PASSWORD))).andExpect(status().isCreated());
    }

    private static String login(MockMvc mockMvc, String nickname) throws Exception {
        String email = email(nickname);
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email": "%s", "password": "%s"}
                """.formatted(email, PASSWORD))).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        return "Bearer " + JsonPath.<String>read(body, "$.accessToken");
    }

    private static String email(String nickname) {
        return nickname + "@example.com";
    }
}
