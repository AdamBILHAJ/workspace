package com.example.team_workspace.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads a local {@code .env} file into system properties so that
 * {@code ./mvnw spring-boot:run} works without exporting variables first.
 *
 * <p>Precedence is real environment variable, then {@code -D} system property,
 * then the {@code .env} file. A value that is already present is never
 * overwritten, so shell and CI configuration always win. Values are never
 * logged. This is a deliberate dependency-free replacement for
 * {@code me.paulschwarz:spring-dotenv}.
 */
public final class DotEnvLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DotEnvLoader.class);
    private static final String FILE_VARIABLE = "SPRING_DOTENV_FILE";
    private static final String DEFAULT_FILE = ".env";
    private static final String EXPORT_PREFIX = "export ";

    private DotEnvLoader() {
    }

    public static void load() {
        Path file = resolveFile();
        int applied = 0;

        if (Files.isRegularFile(file)) {
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (apply(line)) {
                        applied++;
                    }
                }
                LOGGER.info("Loaded {} variable(s) from {}", applied, file);
            } catch (IOException exception) {
                LOGGER.warn("Could not read {}: {}", file, exception.getMessage());
            }
        }

        LOGGER.debug("SPRING_DOTENV_FILE not present, using process environment only");
    }

    private static boolean apply(String rawLine) {
        String line = rawLine.trim();

        if (line.isEmpty() || line.startsWith("#")) {
            return false;
        }

        if (line.startsWith(EXPORT_PREFIX)) {
            line = line.substring(EXPORT_PREFIX.length()).trim();
        }

        int separator = line.indexOf('=');
        if (separator <= 0) {
            return false;
        }

        String key = line.substring(0, separator).trim();
        String value = unquote(line.substring(separator + 1).trim());

        if (key.isEmpty() || value.isEmpty() || isAlreadySet(key)) {
            return false;
        }

        System.setProperty(key, value);
        return true;
    }

    private static boolean isAlreadySet(String key) {
        return System.getenv(key) != null || System.getProperty(key) != null;
    }

    /**
     * Strips a matching pair of surrounding quotes so that values containing
     * {@code #} or spaces survive java.util.Properties comment parsing. Inline
     * comments are deliberately not stripped, which keeps a literal {@code #}
     * inside an unquoted password intact.
     */
    private static String unquote(String value) {
        if (value.length() < 2) {
            return value;
        }

        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);

        if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    private static Path resolveFile() {
        String override = System.getProperty(FILE_VARIABLE);
        if (override == null || override.isBlank()) {
            override = System.getenv(FILE_VARIABLE);
        }

        String fileName = (override == null || override.isBlank())
                ? DEFAULT_FILE
                : override.trim();

        return Path.of(fileName).toAbsolutePath();
    }
}
