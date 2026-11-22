package it.fightersprogram.catalog.domain;

import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.Resource;

public interface CardImageStorage {

    Map<String, String> CONTENT_TYPES_BY_EXTENSION = Map.of(".webp", "image/webp", ".png", "image/png");

    Optional<StoredImage> load(String fileName);

    boolean exists(String fileName);

    void save(String fileName, byte[] content);

    static Optional<String> contentTypeOf(String fileName) {
        return CONTENT_TYPES_BY_EXTENSION.entrySet().stream()
                .filter(entry -> fileName.endsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    record StoredImage(Resource resource, String contentType, long lastModified, long size) {
    }
}
