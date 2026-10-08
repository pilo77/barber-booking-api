package com.villamil.barberbooking.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Dependency guard for the source layers, without adding a test library. */
class HexagonalArchitectureTest {
    private static final Path ROOT = Path.of("src/main/java/com/villamil/barberbooking");

    @Test void domainHasNoFrameworkOrOuterLayerDependencies() throws Exception {
        assertForbiddenImports("domain", "org.springframework.", "jakarta.persistence.", "jakarta.servlet.",
                "com.villamil.barberbooking.application.", "com.villamil.barberbooking.infrastructure.");
    }

    @Test void applicationDoesNotDependOnInfrastructureOrJpa() throws Exception {
        assertForbiddenImports("application", "com.villamil.barberbooking.infrastructure.",
                "jakarta.persistence.", "org.springframework.data.jpa.", "org.springframework.jdbc.",
                "org.springframework.web.");
    }

    private void assertForbiddenImports(String layer, String... forbidden) throws Exception {
        try (var paths = Files.walk(ROOT.resolve(layer))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(path)) {
                    if (!line.stripLeading().startsWith("import ")) continue;
                    for (String dependency : forbidden) {
                        assertThat(line).as("Dependency in %s", path).doesNotContain(dependency);
                    }
                }
            }
        }
    }
}
