package com.example.team_workspace.workspace.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class SlugService {

    private static final int MAX_BASE_LENGTH = 100;
    private static final int MAX_ATTEMPTS = 1000;
    private static final Pattern NON_SLUG_CHARACTERS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-|-$)");

    public String generateUnique(String name, String fallback, Predicate<String> slugExists) {
        String base = toBase(name, fallback);
        if (!slugExists.test(base)) {
            return base;
        }

        for (int suffix = 2; suffix <= MAX_ATTEMPTS; suffix++) {
            String candidate = base + "-" + suffix;
            if (!slugExists.test(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Unable to generate a unique slug for: " + name);
    }

    private String toBase(String name, String fallback) {
        String normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String slug = EDGE_HYPHENS
                .matcher(NON_SLUG_CHARACTERS.matcher(normalized).replaceAll("-"))
                .replaceAll("");

        if (slug.isBlank()) {
            return fallback;
        }
        if (slug.length() > MAX_BASE_LENGTH) {
            slug = EDGE_HYPHENS
                    .matcher(slug.substring(0, MAX_BASE_LENGTH))
                    .replaceAll("");
        }
        return slug.isBlank() ? fallback : slug;
    }
}
