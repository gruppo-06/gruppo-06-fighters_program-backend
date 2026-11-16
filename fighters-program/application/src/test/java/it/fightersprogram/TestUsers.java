package it.fightersprogram;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

final class TestUsers {

    private static final String PASSWORD = "correct-horse-1";

    private TestUsers() {
    }

    static String registerAndLogin(MockMvc mockMvc, String nickname) throws Exception {
        String email = nickname + "@example.com";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"nickname": "%s", "email": "%s", "password": "%s"}
                """.formatted(nickname, email, PASSWORD))).andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email": "%s", "password": "%s"}
                """.formatted(email, PASSWORD))).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        return "Bearer " + JsonPath.<String>read(body, "$.accessToken");
    }
}
