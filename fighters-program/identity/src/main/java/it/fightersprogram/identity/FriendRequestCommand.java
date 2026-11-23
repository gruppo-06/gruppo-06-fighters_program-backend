package it.fightersprogram.identity;

import jakarta.validation.constraints.NotBlank;

public record FriendRequestCommand(@NotBlank String nickname) {
}
