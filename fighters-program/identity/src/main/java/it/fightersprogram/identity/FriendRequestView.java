package it.fightersprogram.identity;

import java.time.OffsetDateTime;

public record FriendRequestView(long id, String nickname, OffsetDateTime createdAt) {
}
