package it.fightersprogram.identity;

import java.time.OffsetDateTime;

public record FriendView(String nickname, OffsetDateTime since) {
}
