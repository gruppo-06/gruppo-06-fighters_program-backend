package it.fightersprogram;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FriendApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aRequestBecomesAFriendshipOnceAccepted() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");
        String bob = TestUsers.registerAndLogin(mockMvc, "friend_bob");

        long requestId = sendRequest(alice, "friend_bob")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname", is("friend_bob")))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString().transform(this::idOf);

        mockMvc.perform(get("/api/me/friends/requests").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incoming", hasSize(1)))
                .andExpect(jsonPath("$.incoming[0].id", is((int) requestId)))
                .andExpect(jsonPath("$.incoming[0].nickname", is("friend_alice")))
                .andExpect(jsonPath("$.outgoing", empty()));

        mockMvc.perform(get("/api/me/friends/requests").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incoming", empty()))
                .andExpect(jsonPath("$.outgoing", hasSize(1)))
                .andExpect(jsonPath("$.outgoing[0].nickname", is("friend_bob")));

        mockMvc.perform(post("/api/me/friends/requests/" + requestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname", is("friend_alice")))
                .andExpect(jsonPath("$.since").isNotEmpty());

        mockMvc.perform(get("/api/me/friends").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nickname", is("friend_bob")));
        mockMvc.perform(get("/api/me/friends").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nickname", is("friend_alice")));
        mockMvc.perform(get("/api/me/friends/requests").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(jsonPath("$.outgoing", empty()));

        mockMvc.perform(delete("/api/me/friends/friend_bob").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/me/friends").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void friendsAreListedSortedByNickname() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");
        String carol = TestUsers.registerAndLogin(mockMvc, "friend_carol");
        String bob = TestUsers.registerAndLogin(mockMvc, "friend_bob");
        acceptFriendship(alice, carol, "friend_carol");
        acceptFriendship(alice, bob, "friend_bob");

        mockMvc.perform(get("/api/me/friends").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nickname", contains("friend_bob", "friend_carol")));
    }

    @Test
    void requestsAreListedNewestFirst() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");
        String bob = TestUsers.registerAndLogin(mockMvc, "friend_bob");
        String carol = TestUsers.registerAndLogin(mockMvc, "friend_carol");
        sendRequest(bob, "friend_alice").andExpect(status().isCreated());
        sendRequest(carol, "friend_alice").andExpect(status().isCreated());

        mockMvc.perform(get("/api/me/friends/requests").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(jsonPath("$.incoming[*].nickname", contains("friend_carol", "friend_bob")));
    }

    @Test
    void declineDeletesTheRequestForBothSides() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");
        String bob = TestUsers.registerAndLogin(mockMvc, "friend_bob");
        long requestId = requestId(sendRequest(alice, "friend_bob").andExpect(status().isCreated()));

        mockMvc.perform(post("/api/me/friends/requests/" + requestId + "/decline")
                        .header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/me/friends/requests").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(jsonPath("$.outgoing", empty()));
        mockMvc.perform(get("/api/me/friends/requests").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(jsonPath("$.incoming", empty()));

        long secondId = requestId(sendRequest(alice, "friend_bob").andExpect(status().isCreated()));
        mockMvc.perform(post("/api/me/friends/requests/" + secondId + "/decline")
                        .header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/me/friends/requests").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(jsonPath("$.incoming", empty()));
    }

    @Test
    void aThirdUserCannotAcceptOrDeclineSomeoneElsesRequest() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");
        TestUsers.registerAndLogin(mockMvc, "friend_bob");
        String carol = TestUsers.registerAndLogin(mockMvc, "friend_carol");
        long requestId = requestId(sendRequest(alice, "friend_bob").andExpect(status().isCreated()));

        mockMvc.perform(post("/api/me/friends/requests/" + requestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, carol))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/me/friends/requests/" + requestId + "/decline")
                        .header(HttpHeaders.AUTHORIZATION, carol))
                .andExpect(status().isNotFound());
    }

    @Test
    void theRequesterCannotAcceptItsOwnRequest() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");
        TestUsers.registerAndLogin(mockMvc, "friend_bob");
        long requestId = requestId(sendRequest(alice, "friend_bob").andExpect(status().isCreated()));

        mockMvc.perform(post("/api/me/friends/requests/" + requestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptingTwiceConflicts() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");
        String bob = TestUsers.registerAndLogin(mockMvc, "friend_bob");
        long requestId = requestId(sendRequest(alice, "friend_bob").andExpect(status().isCreated()));
        mockMvc.perform(post("/api/me/friends/requests/" + requestId + "/accept")
                .header(HttpHeaders.AUTHORIZATION, bob)).andExpect(status().isOk());

        mockMvc.perform(post("/api/me/friends/requests/" + requestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));
    }

    @Test
    void requestingAnUnknownNicknameIsNotFound() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");

        sendRequest(alice, "friend_nobody")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    void requestingYourselfIsABadRequest() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");

        sendRequest(alice, "friend_alice")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    void aBlankNicknameIsAValidationError() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");

        mockMvc.perform(post("/api/me/friends/requests").header(HttpHeaders.AUTHORIZATION, alice)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void aPendingRequestInEitherDirectionConflicts() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");
        String bob = TestUsers.registerAndLogin(mockMvc, "friend_bob");
        sendRequest(alice, "friend_bob").andExpect(status().isCreated());

        sendRequest(alice, "friend_bob")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));
        sendRequest(bob, "friend_alice")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));
    }

    @Test
    void alreadyFriendsConflicts() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");
        String bob = TestUsers.registerAndLogin(mockMvc, "friend_bob");
        acceptFriendship(alice, bob, "friend_bob");

        sendRequest(bob, "friend_alice")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));
    }

    @Test
    void removingAFriendTwiceOrAStrangerIsNotFound() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");
        TestUsers.registerAndLogin(mockMvc, "friend_bob");

        mockMvc.perform(delete("/api/me/friends/friend_bob").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isNotFound());
    }

    @Test
    void aPendingRequestIsNotAFriendshipToRemove() throws Exception {
        String alice = TestUsers.registerAndLogin(mockMvc, "friend_alice");
        TestUsers.registerAndLogin(mockMvc, "friend_bob");
        sendRequest(alice, "friend_bob").andExpect(status().isCreated());

        mockMvc.perform(delete("/api/me/friends/friend_bob").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isNotFound());
    }

    @Test
    void theFriendApiNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/me/friends"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        mockMvc.perform(get("/api/me/friends/requests"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions sendRequest(String token, String nickname) throws Exception {
        return mockMvc.perform(post("/api/me/friends/requests").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\": \"%s\"}".formatted(nickname)));
    }

    private void acceptFriendship(String requesterToken, String addresseeToken, String addresseeNickname)
            throws Exception {
        long requestId = requestId(sendRequest(requesterToken, addresseeNickname).andExpect(status().isCreated()));
        mockMvc.perform(post("/api/me/friends/requests/" + requestId + "/accept")
                .header(HttpHeaders.AUTHORIZATION, addresseeToken)).andExpect(status().isOk());
    }

    private long requestId(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString().transform(this::idOf);
    }

    private long idOf(String body) {
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
