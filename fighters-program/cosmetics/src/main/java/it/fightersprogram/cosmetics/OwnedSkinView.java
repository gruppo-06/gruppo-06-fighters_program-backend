package it.fightersprogram.cosmetics;

import java.time.OffsetDateTime;

public record OwnedSkinView(SkinView skin, OffsetDateTime acquiredAt) {
}
