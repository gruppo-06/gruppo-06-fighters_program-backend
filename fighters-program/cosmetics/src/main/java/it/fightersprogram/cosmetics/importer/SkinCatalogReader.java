package it.fightersprogram.cosmetics.importer;

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
public class SkinCatalogReader {

    private final JsonMapper jsonMapper;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public SkinCatalogReader(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public List<SkinEntry> readAll(String locationPattern) {
        List<SkinEntry> skins = new ArrayList<>();
        for (Resource resource : findResources(locationPattern)) {
            try (InputStream json = resource.getInputStream()) {
                SkinCatalogFile file = jsonMapper.readValue(json, SkinCatalogFile.class);
                skins.addAll(file.skins());
            } catch (JacksonException exception) {
                throw new SkinImportException("Skin catalog file " + resource.getFilename()
                        + " is not valid JSON: " + exception.getOriginalMessage(), exception);
            } catch (IOException exception) {
                throw new SkinImportException("Cannot open skin catalog file " + resource.getFilename(), exception);
            }
        }
        return List.copyOf(skins);
    }

    private Resource[] findResources(String locationPattern) {
        try {
            Resource[] resources = resolver.getResources(locationPattern);
            Arrays.sort(resources, Comparator.comparing(Resource::getFilename));
            return resources;
        } catch (IOException exception) {
            throw new SkinImportException("Cannot list skin catalog files at " + locationPattern, exception);
        }
    }
}
