package it.fightersprogram.cosmetics.domain;

import java.io.Serializable;

public record UserCardVariantId(Long userId, Long cardId) implements Serializable {

    private static final long serialVersionUID = 1L;
}
