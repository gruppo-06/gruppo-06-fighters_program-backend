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

import it.fightersprogram.economy.CreditService;
import it.fightersprogram.economy.CreditType;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CreditApiTest {

    private static final String CREDITS = "/api/me/credits";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CreditService creditService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void showsTheBalanceAndTheLatestTwentyMovementsNewestFirst() throws Exception {
        String token = TestUsers.registerAndLogin(mockMvc, "credit_api");
        long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE nickname = 'credit_api'", Long.class);
        long balanceBefore = creditService.balance(userId);
        for (int day = 1; day <= 25; day++) {
            creditService.credit(userId, 10, CreditType.DAILY, "daily:credit_api:" + day, "day " + day);
        }
        creditService.debit(userId, 40, CreditType.PACK_PURCHASE, "pack:credit_api:1", "Bustina base");

        mockMvc.perform(get(CREDITS).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is((int) balanceBefore + 210)))
                .andExpect(jsonPath("$.movements", hasSize(20)))
                .andExpect(jsonPath("$.movements[0].amount", is(-40)))
                .andExpect(jsonPath("$.movements[0].type", is("PACK_PURCHASE")))
                .andExpect(jsonPath("$.movements[0].reference", is("Bustina base")))
                .andExpect(jsonPath("$.movements[1].reference", is("day 25")))
                .andExpect(jsonPath("$.movements[0].createdAt").isString());
    }

    @Test
    void eachUserSeesOnlyTheirOwnCredits() throws Exception {
        TestUsers.registerAndLogin(mockMvc, "credit_rich");
        String poor = TestUsers.registerAndLogin(mockMvc, "credit_poor");
        long richId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE nickname = 'credit_rich'", Long.class);
        long poorId = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE nickname = 'credit_poor'", Long.class);
        creditService.credit(richId, 1000, CreditType.ADMIN_GRANT, "grant:credit_rich");

        mockMvc.perform(get(CREDITS).header(HttpHeaders.AUTHORIZATION, poor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is((int) creditService.balance(poorId))));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(CREDITS)).andExpect(status().isUnauthorized());
    }
}
