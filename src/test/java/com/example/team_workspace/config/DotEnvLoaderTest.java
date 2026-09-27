package com.example.team_workspace.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class DotEnvLoaderTest {

    private static final List<String> TOUCHED = List.of(
            "DOTENV_TEST_PLAIN",
            "DOTENV_TEST_QUOTED",
            "DOTENV_TEST_HASH",
            "DOTENV_TEST_EXPORTED",
            "DOTENV_TEST_OVERRIDE"
    );

    @AfterEach
    void clearSystemProperties() {
        TOUCHED.forEach(System::clearProperty);
        System.clearProperty("SPRING_DOTENV_FILE");
    }

    /**
     * Every case points the loader at a temp file so the real .env in the
     * project directory is never read, and no secret leaks into the shared
     * surefire JVM.
     */
    private static void pointLoaderAt(Path file) {
        System.setProperty("SPRING_DOTENV_FILE", file.toString());
    }

    @Test
    void loadsPlainQuotedExportedAndHashValues(@TempDir Path directory) throws IOException {
        Path file = directory.resolve(".env");
        Files.writeString(file, String.join("\n",
                "# a leading comment",
                "",
                "DOTENV_TEST_PLAIN=plain-value",
                "  DOTENV_TEST_QUOTED = 'value with spaces'  ",
                "DOTENV_TEST_HASH=pa#ss",
                "export DOTENV_TEST_EXPORTED=exported-value",
                "MALFORMED_LINE_WITHOUT_SEPARATOR"
        ), StandardCharsets.UTF_8);
        pointLoaderAt(file);

        DotEnvLoader.load();

        assertThat(System.getProperty("DOTENV_TEST_PLAIN")).isEqualTo("plain-value");
        assertThat(System.getProperty("DOTENV_TEST_QUOTED")).isEqualTo("value with spaces");
        assertThat(System.getProperty("DOTENV_TEST_HASH")).isEqualTo("pa#ss");
        assertThat(System.getProperty("DOTENV_TEST_EXPORTED")).isEqualTo("exported-value");
        assertThat(System.getProperty("MALFORMED_LINE_WITHOUT_SEPARATOR")).isNull();
    }

    @Test
    void neverOverwritesAValueAlreadyPresent(@TempDir Path directory) throws IOException {
        Path file = directory.resolve(".env");
        Files.writeString(file, "DOTENV_TEST_OVERRIDE=from-file", StandardCharsets.UTF_8);
        pointLoaderAt(file);
        System.setProperty("DOTENV_TEST_OVERRIDE", "from-system-property");

        DotEnvLoader.load();

        assertThat(System.getProperty("DOTENV_TEST_OVERRIDE")).isEqualTo("from-system-property");
    }

    @Test
    void missingFileIsIgnored(@TempDir Path directory) {
        pointLoaderAt(directory.resolve("absent"));

        DotEnvLoader.load();

        assertThat(System.getProperty("DOTENV_TEST_PLAIN")).isNull();
    }
}
