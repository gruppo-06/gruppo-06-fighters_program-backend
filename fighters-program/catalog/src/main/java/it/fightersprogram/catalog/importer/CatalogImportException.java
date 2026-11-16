package it.fightersprogram.catalog.importer;

public class CatalogImportException extends RuntimeException {

    public CatalogImportException(String message) {
        super(message);
    }

    public CatalogImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
