package it.fightersprogram.identity;

import java.util.List;

public record FriendRequests(List<FriendRequestView> incoming, List<FriendRequestView> outgoing) {
}
