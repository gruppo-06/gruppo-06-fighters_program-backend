package it.fightersprogram.cosmetics.importer;

import java.util.List;

public record SkinCatalogFile(List<SkinEntry> skins) {

    public SkinCatalogFile {
        skins = skins == null ? List.of() : List.copyOf(skins);
    }
}
