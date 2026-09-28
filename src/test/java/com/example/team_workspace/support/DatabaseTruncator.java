package com.example.team_workspace.support;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Empties the shared H2 database between tests.
 *
 * <p>Two things make this necessary rather than optional. The database is
 * shared by every integration test, so rows committed by one test are visible
 * to the next. And tests that need {@code AFTER_COMMIT} listeners cannot be
 * {@code @Transactional}, so nothing rolls their data back: without an explicit
 * wipe, every task, comment and notification a test creates stays resident in
 * the shared {@code EntityManagerFactory} and in the Hikari statement cache for
 * the remainder of the suite.
 *
 * <p>Call {@link #truncate} from both {@code @BeforeEach} and {@code @AfterEach}.
 * The first call isolates a test from whatever ran before it; the second stops
 * it from being counted as somebody else's setup, and releases the rows it made.
 */
public final class DatabaseTruncator {

    /**
     * Child tables first. {@code SET REFERENTIAL_INTEGRITY FALSE} means the
     * order is not enforced, but keeping it correct means a future foreign-key
     * check during truncate cannot surprise us.
     */
    private static final List<String> TABLES = List.of(
            "notifications",
            "activity_logs",
            "task_comments",
            "tasks",
            "kanban_columns",
            "projects",
            "workspace_members",
            "workspaces",
            "organizations",
            "users"
    );

    private DatabaseTruncator() {
    }

    public static void truncate(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            for (String table : TABLES) {
                jdbcTemplate.execute("TRUNCATE TABLE " + table);
            }
        } finally {
            // Always restored: a leaked session would corrupt every later test
            // in a way that looks like an application bug.
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }
}
