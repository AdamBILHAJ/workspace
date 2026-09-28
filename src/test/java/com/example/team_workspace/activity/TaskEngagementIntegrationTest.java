package com.example.team_workspace.activity;

import java.util.List;
import java.util.Map;

import com.example.team_workspace.support.DatabaseTruncator;
import com.example.team_workspace.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deliberately not @Transactional: the activity and notification listeners run
 * on AFTER_COMMIT, so these tests need real commits. State is truncated on both
 * sides of each test instead of rolled back, so nothing survives into the
 * shared database. See {@link IntegrationTest} for the shared context.
 */
@IntegrationTest
class TaskEngagementIntegrationTest {

    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        DatabaseTruncator.truncate(jdbcTemplate);
    }

    @AfterEach
    void releaseDatabase() {
        DatabaseTruncator.truncate(jdbcTemplate);
    }

    @Test
    void creatingATaskRecordsActivityAndNotifiesTheAssignee() throws Exception {
        Fixture fixture = new Fixture();
        fixture.signUp("owner@engage.io", "owner@engage.io");
        fixture.signUp("mate@engage.io", "mate@engage.io");
        fixture.createWorkspace();
        fixture.addMember("mate@engage.io");
        long columnId = fixture.firstColumnId();
        long projectId = fixture.createProject("Mobile App");

        long taskId = fixture.createTask(columnId, "Draft the RFC", fixture.userId("mate@engage.io"));

        List<String> actions = activityActions(taskId, fixture.token("owner@engage.io"));
        assertThat(actions).containsExactly("TASK_CREATED");
        assertThat(activityDetails(taskId, fixture.token("owner@engage.io")))
                .anySatisfy(details -> assertThat(details).isEqualTo("Created in To Do"));

        JsonNode notifications = notifications(fixture.token("mate@engage.io"));
        assertThat(notifications.path("unreadCount").asLong()).isEqualTo(1);
        JsonNode first = notifications.path("notifications").get(0);
        assertThat(first.path("title").asText()).isEqualTo("New task assigned to you");
        assertThat(first.path("message").asText()).isEqualTo("Draft the RFC was assigned to you.");
        assertThat(first.path("isRead").asBoolean()).isFalse();
        assertThat(first.path("targetUrl").asText())
                .isEqualTo("/workspaces/delivery/projects/MOBILE?task=" + taskId);

        assertThat(notifications(fixture.token("owner@engage.io")).path("notifications").isEmpty()).isTrue();
        assertThat(projectId).isPositive();
    }

    @Test
    void movingATaskBetweenColumnsIsAudited() throws Exception {
        Fixture fixture = new Fixture();
        fixture.signUp("owner@engage.io", "owner@engage.io");
        fixture.createWorkspace();
        long toDo = fixture.columnId("To Do");
        long inProgress = fixture.columnId("In Progress");
        long taskId = fixture.createTask(toDo, "Ship it", null);

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/move", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.token("owner@engage.io")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("columnId", inProgress, "orderIndex", 0))))
                .andExpect(status().isOk());

        String token = fixture.token("owner@engage.io");
        assertThat(activityActions(taskId, token)).containsExactly("TASK_MOVED", "TASK_CREATED");
        assertThat(activityDetails(taskId, token))
                .anySatisfy(details -> assertThat(details).isEqualTo("Moved from To Do to In Progress"));
    }

    @Test
    void reorderingInsideAColumnIsNotLoggedAsAMove() throws Exception {
        Fixture fixture = new Fixture();
        fixture.signUp("owner@engage.io", "owner@engage.io");
        fixture.createWorkspace();
        long toDo = fixture.columnId("To Do");
        long first = fixture.createTask(toDo, "First", null);
        long second = fixture.createTask(toDo, "Second", null);

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/move", second)
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.token("owner@engage.io")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("columnId", toDo, "orderIndex", 0))))
                .andExpect(status().isOk());

        assertThat(activityActions(first, fixture.token("owner@engage.io")))
                .containsExactly("TASK_CREATED");
    }

    @Test
    void commentingStoresTheCommentAuditsItAndNotifiesTheAssignee() throws Exception {
        Fixture fixture = new Fixture();
        fixture.signUp("owner@engage.io", "owner@engage.io");
        fixture.signUp("mate@engage.io", "mate@engage.io");
        fixture.signUp("third@engage.io", "Third");
        fixture.createWorkspace();
        fixture.addMember("mate@engage.io");
        fixture.addMember("third@engage.io");
        long columnId = fixture.firstColumnId();
        long taskId = fixture.createTask(columnId, "Review the API", fixture.userId("mate@engage.io"));

        String ownerToken = fixture.token("owner@engage.io");
        String thirdToken = fixture.token("third@engage.io");

        mockMvc.perform(post("/api/v1/tasks/{taskId}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(thirdToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "  Looks good, one nit.  "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Looks good, one nit."))
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.authorEmail").value("third@engage.io"))
                .andExpect(jsonPath("$.authorName").value("Third User"));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("Looks good, one nit."));

        assertThat(activityActions(taskId, ownerToken))
                .containsExactly("COMMENT_ADDED", "TASK_CREATED");

        JsonNode mateNotifications = notifications(fixture.token("mate@engage.io"));
        assertThat(mateNotifications.path("unreadCount").asLong()).isEqualTo(2);
        assertThat(mateNotifications.path("notifications").get(0).path("title").asText())
                .isEqualTo("New comment on your task");
        assertThat(mateNotifications.path("notifications").get(0).path("message").asText())
                .isEqualTo("Third User commented on Review the API.");

        assertThat(notifications(thirdToken).path("notifications").isEmpty()).isTrue();
    }

    @Test
    void commentsAreReturnedInChronologicalOrder() throws Exception {
        Fixture fixture = new Fixture();
        fixture.signUp("owner@engage.io", "owner@engage.io");
        fixture.createWorkspace();
        long taskId = fixture.createTask(fixture.firstColumnId(), "Ordered", null);
        String token = fixture.token("owner@engage.io");

        for (String content : List.of("first", "second", "third")) {
            mockMvc.perform(post("/api/v1/tasks/{taskId}/comments", taskId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("content", content))))
                    .andExpect(status().isCreated());
        }

        String response = mockMvc.perform(get("/api/v1/tasks/{taskId}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(response).findValuesAsText("content"))
                .containsExactly("first", "second", "third");
    }

    @Test
    void notificationsCanBeMarkedReadIndividuallyAndInBulk() throws Exception {
        Fixture fixture = new Fixture();
        fixture.signUp("owner@engage.io", "owner@engage.io");
        fixture.signUp("mate@engage.io", "mate@engage.io");
        fixture.createWorkspace();
        fixture.addMember("mate@engage.io");
        long columnId = fixture.firstColumnId();
        String mateToken = fixture.token("mate@engage.io");

        fixture.createTask(columnId, "One", fixture.userId("mate@engage.io"));
        fixture.createTask(columnId, "Two", fixture.userId("mate@engage.io"));

        JsonNode all = notifications(mateToken);
        assertThat(all.path("unreadCount").asLong()).isEqualTo(2);
        long firstId = all.path("notifications").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", firstId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(mateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));

        assertThat(notifications(mateToken).path("unreadCount").asLong()).isEqualTo(1);

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, bearer(mateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(1));

        JsonNode cleared = notifications(mateToken);
        assertThat(cleared.path("unreadCount").asLong()).isZero();
        assertThat(cleared.path("notifications").findValuesAsText("isRead"))
                .isNotEmpty()
                .allSatisfy(value -> assertThat(value).isEqualTo("true"));
    }

    @Test
    void notificationsAreScopedToTheRecipient() throws Exception {
        Fixture fixture = new Fixture();
        fixture.signUp("owner@engage.io", "owner@engage.io");
        fixture.signUp("mate@engage.io", "mate@engage.io");
        fixture.signUp("stranger@engage.io", "stranger@engage.io");
        fixture.createWorkspace();
        fixture.addMember("mate@engage.io");
        long taskId = fixture.createTask(
                fixture.firstColumnId(),
                "Private",
                fixture.userId("mate@engage.io")
        );

        JsonNode mateNotifications = notifications(fixture.token("mate@engage.io"));
        long notificationId = mateNotifications.path("notifications").get(0).path("id").asLong();

        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.token("stranger@engage.io"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isEmpty())
                .andExpect(jsonPath("$.unreadCount").value(0));

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.token("stranger@engage.io"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found"));

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId))
                .andExpect(status().isUnauthorized());

        assertThat(notifications(fixture.token("mate@engage.io")).path("unreadCount").asLong())
                .isEqualTo(1);
        assertThat(taskId).isPositive();
    }

    @Test
    void engagementEndpointsEnforceWorkspaceMembership() throws Exception {
        Fixture fixture = new Fixture();
        fixture.signUp("owner@engage.io", "owner@engage.io");
        fixture.signUp("outsider@engage.io", "outsider@engage.io");
        fixture.createWorkspace();
        long taskId = fixture.createTask(fixture.firstColumnId(), "Members only", null);

        String outsiderToken = fixture.token("outsider@engage.io");

        mockMvc.perform(get("/api/v1/tasks/{taskId}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/tasks/{taskId}/activity", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/tasks/{taskId}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "let me in"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/tasks/{taskId}/comments", taskId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/tasks/{taskId}/comments", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.token("owner@engage.io"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found"));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/activity", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(fixture.token("owner@engage.io"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void commentPayloadsAreValidated() throws Exception {
        Fixture fixture = new Fixture();
        fixture.signUp("owner@engage.io", "owner@engage.io");
        fixture.createWorkspace();
        long taskId = fixture.createTask(fixture.firstColumnId(), "Validate me", null);
        String token = fixture.token("owner@engage.io");

        mockMvc.perform(post("/api/v1/tasks/{taskId}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.content").isNotEmpty());

        mockMvc.perform(post("/api/v1/tasks/{taskId}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "x".repeat(4001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.content").isNotEmpty());

        assertThat(taskId).isPositive();
    }

    @Test
    void aTaskWithNoAssigneeDoesNotNotifyTheReporter() throws Exception {
        Fixture fixture = new Fixture();
        fixture.signUp("owner@engage.io", "owner@engage.io");
        fixture.signUp("mate@engage.io", "mate@engage.io");
        fixture.createWorkspace();
        fixture.addMember("mate@engage.io");
        long taskId = fixture.createTask(fixture.firstColumnId(), "Unassigned", null);

        String mateToken = fixture.token("mate@engage.io");
        mockMvc.perform(post("/api/v1/tasks/{taskId}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(mateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "I will take this"))))
                .andExpect(status().isCreated());

        assertThat(notifications(mateToken).path("notifications").isEmpty()).isTrue();
        assertThat(notifications(fixture.token("owner@engage.io")).path("notifications").isEmpty())
                .isTrue();
    }

    private JsonNode notifications(String token) throws Exception {
        String response = mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private List<String> activityActions(long taskId, String token) throws Exception {
        String response = mockMvc.perform(get("/api/v1/tasks/{taskId}/activity", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).findValuesAsText("action");
    }

    private List<String> activityDetails(long taskId, String token) throws Exception {
        String response = mockMvc.perform(get("/api/v1/tasks/{taskId}/activity", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).findValuesAsText("details");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Map<String, ?> payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    /**
     * Small builder for the signup, workspace and project scaffolding that
     * every scenario in this suite needs.
     */
    private final class Fixture {

        private final Map<String, String> tokens = new java.util.HashMap<>();
        private final Map<String, Long> userIds = new java.util.HashMap<>();
        private String firstSignupEmail;
        private Long workspaceId;
        private Long projectId;
        private Map<String, Long> columnIds = Map.of();

        void signUp(String email, String firstName) throws Exception {
            String response = mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", email,
                                    "password", PASSWORD,
                                    "firstName", firstName,
                                    "lastName", "User"
                            ))))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            JsonNode body = objectMapper.readTree(response);
            if (firstSignupEmail == null) {
                firstSignupEmail = email;
            }
            tokens.put(email, body.path("accessToken").asText());
            userIds.put(email, body.path("userSummary").path("id").asLong());
        }

        void createWorkspace() throws Exception {
            String owner = ownerEmail();
            long organizationId = objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokens.get(owner)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("name", "Engagement Org " + owner))))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString()).path("id").asLong();

            workspaceId = objectMapper.readTree(mockMvc.perform(
                            post("/api/v1/organizations/{organizationId}/workspaces", organizationId)
                                    .header(HttpHeaders.AUTHORIZATION, bearer(tokens.get(owner)))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json(Map.of("name", "Delivery"))))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString()).path("id").asLong();

            createProject("Mobile App");
        }

        long createProject(String name) throws Exception {
            String owner = ownerEmail();
            projectId = objectMapper.readTree(mockMvc.perform(
                            post("/api/v1/workspaces/{workspaceId}/projects", workspaceId)
                                    .header(HttpHeaders.AUTHORIZATION, bearer(tokens.get(owner)))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json(Map.of("name", name))))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString()).path("id").asLong();

            return projectId;
        }

        void addMember(String email) throws Exception {
            mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokens.get(ownerEmail())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", email))))
                    .andExpect(status().isCreated());
        }

        long firstColumnId() throws Exception {
            if (columnIds.isEmpty()) {
                columnIds = readColumns(tokens.get(ownerEmail()));
            }
            return columnIds.get("To Do");
        }

        long columnId(String name) throws Exception {
            if (columnIds.isEmpty()) {
                columnIds = readColumns(tokens.get(ownerEmail()));
            }
            return columnIds.get(name);
        }

        private Map<String, Long> readColumns(String token) throws Exception {
            String response = mockMvc.perform(get("/api/v1/projects/{projectId}/board", projectId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Map<String, Long> result = new java.util.HashMap<>();
            objectMapper.readTree(response).path("columns")
                    .forEach(column -> result.put(column.path("name").asText(), column.path("id").asLong()));
            return result;
        }

        long createTask(long columnId, String title, Long assigneeId) throws Exception {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("title", title);
            payload.put("priority", "MEDIUM");
            if (assigneeId != null) {
                payload.put("assigneeId", assigneeId);
            }

            return objectMapper.readTree(mockMvc.perform(
                            post("/api/v1/columns/{columnId}/tasks", columnId)
                                    .header(HttpHeaders.AUTHORIZATION, bearer(tokens.get(ownerEmail())))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json(payload)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString()).path("id").asLong();
        }

        private String ownerEmail() {
            return firstSignupEmail;
        }

        String token(String email) {
            return tokens.get(email);
        }

        long userId(String email) {
            return userIds.get(email);
        }
    }
}
