package it.fightersprogram.catalog.importer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class CatalogReader {

    private final JsonMapper jsonMapper;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public CatalogReader(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public List<CatalogFile> readAll(String locationPattern) {
        List<CatalogFile> files = new ArrayList<>();
        for (Resource resource : findResources(locationPattern)) {
            try (InputStream json = resource.getInputStream()) {
                files.add(read(json, resource.getFilename()));
            } catch (IOException exception) {
                throw new CatalogImportException("Cannot open catalog file " + resource.getFilename(), exception);
            }
        }
        return files;
    }

    public CatalogFile read(InputStream json, String fileName) {
        try {
            return jsonMapper.readValue(json, CatalogFile.class);
        } catch (JacksonException exception) {
            throw new CatalogImportException(
                    "Catalog file " + fileName + " is not valid JSON: " + exception.getOriginalMessage(), exception);
        }
    }

    private Resource[] findResources(String locationPattern) {
        try {
            Resource[] resources = resolver.getResources(locationPattern);
            Arrays.sort(resources, Comparator.comparing(Resource::getFilename));
            return resources;
        } catch (IOException exception) {
            throw new CatalogImportException("Cannot list catalog files at " + locationPattern, exception);
        }
    }
}
