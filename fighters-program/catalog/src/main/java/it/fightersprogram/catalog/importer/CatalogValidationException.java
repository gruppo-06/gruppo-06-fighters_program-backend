package it.fightersprogram.catalog.importer;

import java.util.List;

public class CatalogValidationException extends CatalogImportException {

    public CatalogValidationException(List<String> problems) {
        super("The card catalog is not consistent:\n- " + String.join("\n- ", problems));
    }
}
