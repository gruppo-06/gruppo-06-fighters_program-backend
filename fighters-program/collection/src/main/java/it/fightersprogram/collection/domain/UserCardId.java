package it.fightersprogram.collection.domain;

import java.io.Serializable;

public record UserCardId(Long userId, Long cardId) implements Serializable {
}
