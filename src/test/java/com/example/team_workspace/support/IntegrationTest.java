package com.example.team_workspace.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * The single configuration every integration test must use.
 *
 * <p>The Spring TestContext framework caches one {@code ApplicationContext} per
 * distinct set of configuration attributes and keeps it alive until the JVM
 * exits. Every extra variant is therefore a permanently resident copy of the
 * whole object graph: DataSource, EntityManagerFactory, the WebSocket broker,
 * Tomcat, and every bean holding a class and its instances. Sticking to this
 * annotation everywhere collapses all integration tests onto <em>one</em>
 * context, which is the single largest saving available to the test suite.
 *
 * <p>Consequences this deliberately accepts:
 *
 * <ul>
 *   <li>Every integration test uses {@code RANDOM_PORT}, even the REST-only
 *       ones, because {@code MOCK} and {@code RANDOM_PORT} are different context
 *       keys and would each need their own cached context.</li>
 *   <li>All tests share one H2 instance, so anything not rolled back must be
 *       truncated in {@code @AfterEach}; see
 *       {@link com.example.team_workspace.support.DatabaseTruncator}.</li>
 *   <li>No test may add a {@code @MockBean}. Every distinct mock set is a new
 *       context key, which is exactly the accumulation this guards against.
 *       Reshape the data under test instead, or use a plain Mockito mock.</li>
 *   <li>No {@code @DirtiesContext}. It closes and reloads the context, which
 *       costs a full startup per use and leaves the old one eligible for GC
 *       only after the new one is built, so it roughly doubles peak heap.</li>
 * </ul>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // One shared in-memory database. A per-class URL would fork a
                // separate context (and therefore a separate H2) per class.
                "spring.datasource.url=jdbc:h2:mem:team-workspace-test;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                // A small pool: the suite is strictly sequential, so extra
                // connections only reserve memory.
                "spring.datasource.hikari.maximum-pool-size=5",
                "spring.datasource.hikari.minimum-idle=1",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.jpa.show-sql=false",
                "app.jwt.secret=integration-test-signing-key-with-more-than-32-bytes",
                "app.jwt.expiration=1h"
        }
)
@AutoConfigureMockMvc
public @interface IntegrationTest {
}
