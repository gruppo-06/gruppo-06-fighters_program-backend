package it.fightersprogram.cosmetics.importer;

public class SkinImportException extends RuntimeException {

    public SkinImportException(String message) {
        super(message);
    }

    public SkinImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
