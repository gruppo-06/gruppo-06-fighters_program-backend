package it.fightersprogram.engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class EngineIsolationTest {

    private static final Path ENGINE_SOURCES = Path.of("src/main/java/it/fightersprogram/engine");

    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "import org.springframework",
            "import jakarta.",
            "import com.fasterxml",
            "import lombok");

    @Test
    void engineStaysPureJava() throws IOException {
        try (Stream<Path> sources = Files.walk(ENGINE_SOURCES)) {
            List<Path> offenders = sources
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(EngineIsolationTest::importsFramework)
                    .toList();
            assertTrue(offenders.isEmpty(),
                    () -> "The engine must stay pure Java, but these files import a framework: " + offenders);
        }
    }

    private static boolean importsFramework(Path source) {
        try {
            return Files.readString(source).lines().anyMatch(EngineIsolationTest::isForbiddenImport);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static boolean isForbiddenImport(String line) {
        String trimmed = line.strip();
        return FORBIDDEN_IMPORTS.stream().anyMatch(trimmed::startsWith);
    }
}
