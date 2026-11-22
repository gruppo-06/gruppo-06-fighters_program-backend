package it.fightersprogram.catalog.domain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

@Component
public class FileSystemCardImageStorage implements CardImageStorage {

    private final Path directory;

    public FileSystemCardImageStorage(@Value("${catalog.images.location}") String location) {
        this.directory = Path.of(location).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot create the card image directory " + directory, exception);
        }
    }

    @Override
    public Optional<StoredImage> load(String fileName) {
        Path path = pathOf(fileName);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        String contentType = CardImageStorage.contentTypeOf(fileName).orElseThrow();
        try {
            return Optional.of(new StoredImage(new FileSystemResource(path), contentType,
                    Files.getLastModifiedTime(path).toMillis(), Files.size(path)));
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read card image " + fileName, exception);
        }
    }

    @Override
    public boolean exists(String fileName) {
        return Files.isRegularFile(pathOf(fileName));
    }

    @Override
    public void save(String fileName, byte[] content) {
        Path target = pathOf(fileName);
        try {
            Path temporary = Files.createTempFile(directory, "upload-", ".tmp");
            try {
                Files.write(temporary, content);
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot save card image " + fileName, exception);
        }
    }

    private Path pathOf(String fileName) {
        if (fileName == null || fileName.isBlank()
                || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new IllegalArgumentException("Invalid card image file name: " + fileName);
        }
        if (CardImageStorage.contentTypeOf(fileName).isEmpty()) {
            throw new IllegalArgumentException("Unsupported card image type: " + fileName);
        }
        Path path = directory.resolve(fileName).normalize();
        if (!directory.equals(path.getParent())) {
            throw new IllegalArgumentException("Card image outside the storage directory: " + fileName);
        }
        return path;
    }
}
