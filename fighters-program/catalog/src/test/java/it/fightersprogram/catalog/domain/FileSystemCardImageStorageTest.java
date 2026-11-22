package it.fightersprogram.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FileSystemCardImageStorageTest {

    private static final byte[] CONTENT = "RIFF0000WEBP".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path root;

    private FileSystemCardImageStorage storage;

    @BeforeEach
    void createStorage() {
        storage = new FileSystemCardImageStorage(root.resolve("images").toString());
    }

    @Test
    void createsTheDirectoryWhenItIsMissing() {
        assertTrue(Files.isDirectory(root.resolve("images")));
    }

    @Test
    void aSavedImageLoadsWithItsBytesContentTypeAndSize() throws IOException {
        storage.save("AF01.webp", CONTENT);

        CardImageStorage.StoredImage image = storage.load("AF01.webp").orElseThrow();

        assertArrayEquals(CONTENT, image.resource().getContentAsByteArray());
        assertEquals("image/webp", image.contentType());
        assertEquals(CONTENT.length, image.size());
    }

    @Test
    void aPngImageLoadsWithThePngContentType() {
        storage.save("AF01.png", CONTENT);

        assertEquals("image/png", storage.load("AF01.png").orElseThrow().contentType());
    }

    @Test
    void saveReplacesAnExistingImage() throws IOException {
        storage.save("AF01.webp", CONTENT);
        storage.save("AF01.webp", "other".getBytes(StandardCharsets.US_ASCII));

        assertArrayEquals("other".getBytes(StandardCharsets.US_ASCII),
                storage.load("AF01.webp").orElseThrow().resource().getContentAsByteArray());
    }

    @Test
    void existsIsTrueOnlyAfterTheImageIsSaved() {
        assertFalse(storage.exists("AF01.webp"));

        storage.save("AF01.webp", CONTENT);

        assertTrue(storage.exists("AF01.webp"));
    }

    @Test
    void aMissingImageLoadsAsEmpty() {
        assertTrue(storage.load("AF02.webp").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"../x.webp", "a/b.webp", "a\\b.webp", "x.gif", "x", "", "   "})
    void rejectsUnsafeOrUnsupportedNames(String fileName) {
        assertThrows(IllegalArgumentException.class, () -> storage.save(fileName, CONTENT));
        assertThrows(IllegalArgumentException.class, () -> storage.load(fileName));
        assertThrows(IllegalArgumentException.class, () -> storage.exists(fileName));
    }
}
