package it.fightersprogram;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import it.fightersprogram.economy.WelcomeBonus;
import it.fightersprogram.identity.UserRegistered;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WelcomeBonusTest {

    private static final String CREDITS = "/api/me/credits";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WelcomeBonus welcomeBonus;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void grantsOneWelcomeMovementToANewPlayer() throws Exception {
        String token = TestUsers.registerAndLogin(mockMvc, "welcome_new");

        mockMvc.perform(get(CREDITS).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(500)))
                .andExpect(jsonPath("$.movements", hasSize(1)))
                .andExpect(jsonPath("$.movements[0].type", is("WELCOME")))
                .andExpect(jsonPath("$.movements[0].amount", is(500)));
    }

    @Test
    void replayedRegistrationEventDoesNotGrantTheBonusTwice() throws Exception {
        String token = TestUsers.registerAndLogin(mockMvc, "welcome_replay");
        long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE nickname = 'welcome_replay'", Long.class);

        welcomeBonus.grantWelcome(new UserRegistered(userId));

        mockMvc.perform(get(CREDITS).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(500)))
                .andExpect(jsonPath("$.movements", hasSize(1)))
                .andExpect(jsonPath("$.movements[0].type", is("WELCOME")));
    }
}
