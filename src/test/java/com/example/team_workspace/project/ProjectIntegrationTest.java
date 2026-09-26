package com.example.team_workspace.project;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:project-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "app.jwt.secret=integration-test-signing-key-with-more-than-32-bytes",
        "app.jwt.expiration=1h"
})
@AutoConfigureMockMvc
@Transactional
class ProjectIntegrationTest {

    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createProjectSeedsDefaultColumns() throws Exception {
        String ownerToken = signUp("owner@example.com").path("accessToken").asText();
        long workspaceId = createWorkspace(ownerToken, "Engineering Team");

        String response = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/projects", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Mobile App",
                                "description", "Customer facing mobile experience"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("MOBILE"))
                .andExpect(jsonPath("$.name").value("Mobile App"))
                .andExpect(jsonPath("$.workspaceId").value(workspaceId))
                .andExpect(jsonPath("$.totalTasks").value(0))
                .andExpect(jsonPath("$.completionPercent").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long projectId = objectMapper.readTree(response).path("id").asLong();

        JsonNode board = board(projectId, ownerToken);

        assertThat(board.path("project").path("key").asText()).isEqualTo("MOBILE");
        assertThat(columnNames(board)).containsExactly("To Do", "In Progress", "Done");
        assertThat(board.path("columns")).allSatisfy(column ->
                assertThat(column.path("tasks").isEmpty()).isTrue());
    }

    @Test
    void projectKeysAreDerivedAndUnique() throws Exception {
        String token = signUp("keys@example.com").path("accessToken").asText();
        long workspaceId = createWorkspace(token, "Product");

        assertThat(createProject(token, workspaceId, "Mobile App", null).path("key").asText())
                .isEqualTo("MOBILE");
        assertThat(createProject(token, workspaceId, "Mobile App", null).path("key").asText())
                .isEqualTo("MOBILE2");
        assertThat(createProject(token, workspaceId, "Mobile App", null).path("key").asText())
                .isEqualTo("MOBILE3");

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/projects", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Explicit", "key", "MOBILE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("A project with this key already exists in the workspace"));
    }

    @Test
    void listProjectsReportsProgressFromDoneColumn() throws Exception {
        String token = signUp("progress@example.com").path("accessToken").asText();
        long workspaceId = createWorkspace(token, "Delivery");
        long projectId = createProject(token, workspaceId, "Payments", null).path("id").asLong();

        JsonNode board = board(projectId, token);
        long toDo = columnId(board, 0);
        long inProgress = columnId(board, 1);
        long done = columnId(board, 2);

        createTask(toDo, token, "Draft schema", "HIGH", null);
        createTask(inProgress, token, "Build endpoint", "MEDIUM", null);
        createTask(done, token, "Ship it", "URGENT", null);

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/projects", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].totalTasks").value(3))
                .andExpect(jsonPath("$[0].completedTasks").value(1))
                .andExpect(jsonPath("$[0].completionPercent").value(33));
    }

    @Test
    void addColumnAppendsAfterExistingColumns() throws Exception {
        String token = signUp("columns@example.com").path("accessToken").asText();
        long workspaceId = createWorkspace(token, "Review");
        long projectId = createProject(token, workspaceId, "Launch", null).path("id").asLong();

        mockMvc.perform(post("/api/v1/projects/{projectId}/columns", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "In Review"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("In Review"))
                .andExpect(jsonPath("$.orderIndex").value(3))
                .andExpect(jsonPath("$.tasks").isEmpty());

        assertThat(columnNames(board(projectId, token)))
                .containsExactly("To Do", "In Progress", "Done", "In Review");
    }

    @Test
    void createTaskRecordsReporterAndValidatesAssignee() throws Exception {
        String ownerToken = signUp("task-owner@example.com").path("accessToken").asText();
        JsonNode teammate = signUp("teammate@example.com");
        String teammateToken = teammate.path("accessToken").asText();
        JsonNode outsider = signUp("outsider@example.com");

        long workspaceId = createWorkspace(ownerToken, "Tasks");
        long projectId = createProject(ownerToken, workspaceId, "Roadmap", null).path("id").asLong();
        long columnId = columnId(board(projectId, ownerToken), 0);

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "teammate@example.com"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/columns/{columnId}/tasks", columnId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Write the RFC",
                                "description", "Cover the migration plan",
                                "priority", "HIGH",
                                "assigneeId", teammate.path("userSummary").path("id").asLong()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.columnId").value(columnId))
                .andExpect(jsonPath("$.orderIndex").value(0))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assignee.email").value("teammate@example.com"))
                .andExpect(jsonPath("$.reporter.email").value("task-owner@example.com"));

        mockMvc.perform(post("/api/v1/columns/{columnId}/tasks", columnId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teammateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Not for outsiders",
                                "priority", "LOW",
                                "assigneeId", outsider.path("userSummary").path("id").asLong()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("The selected assignee is not a member of this workspace"));

        mockMvc.perform(post("/api/v1/columns/{columnId}/tasks", columnId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teammateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Ghost assignee",
                                "priority", "LOW",
                                "assigneeId", 999999
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The selected assignee does not exist"));
    }

    @Test
    void moveTaskReordersWithinTheSameColumn() throws Exception {
        String token = signUp("reorder@example.com").path("accessToken").asText();
        long workspaceId = createWorkspace(token, "Reorder");
        long projectId = createProject(token, workspaceId, "Sorting", null).path("id").asLong();
        long columnId = columnId(board(projectId, token), 0);

        long first = createTask(columnId, token, "First", "LOW", null).path("id").asLong();
        long second = createTask(columnId, token, "Second", "LOW", null).path("id").asLong();
        long third = createTask(columnId, token, "Third", "LOW", null).path("id").asLong();

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/move", third)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("columnId", columnId, "orderIndex", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderIndex").value(0));

        assertThat(tasksInColumn(board(projectId, token), columnId))
                .containsExactly("Third", "First", "Second");
        assertThat(orderIndexesInColumn(board(projectId, token), columnId))
                .containsExactly(0, 1, 2);

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/move", first)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("columnId", columnId, "orderIndex", 99))))
                .andExpect(status().isOk());

        assertThat(tasksInColumn(board(projectId, token), columnId))
                .containsExactly("Third", "Second", "First");
        assertThat(orderIndexesInColumn(board(projectId, token), columnId))
                .containsExactly(0, 1, 2);
    }

    @Test
    void moveTaskAcrossColumnsKeepsBothColumnsContiguous() throws Exception {
        String token = signUp("cross@example.com").path("accessToken").asText();
        long workspaceId = createWorkspace(token, "Cross");
        long projectId = createProject(token, workspaceId, "Crossing", null).path("id").asLong();
        JsonNode board = board(projectId, token);
        long toDo = columnId(board, 0);
        long inProgress = columnId(board, 1);

        createTask(toDo, token, "A", "LOW", null);
        long taskToMove = createTask(toDo, token, "B", "LOW", null).path("id").asLong();
        createTask(inProgress, token, "C", "LOW", null);
        createTask(inProgress, token, "D", "LOW", null);

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/move", taskToMove)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("columnId", inProgress, "orderIndex", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columnId").value(inProgress))
                .andExpect(jsonPath("$.orderIndex").value(1));

        JsonNode updated = board(projectId, token);
        assertThat(tasksInColumn(updated, toDo)).containsExactly("A");
        assertThat(tasksInColumn(updated, inProgress)).containsExactly("C", "B", "D");
        assertThat(orderIndexesInColumn(updated, inProgress)).containsExactly(0, 1, 2);
    }

    @Test
    void moveTaskRejectsColumnFromAnotherProject() throws Exception {
        String token = signUp("foreign@example.com").path("accessToken").asText();
        long workspaceId = createWorkspace(token, "Foreign");
        long firstProject = createProject(token, workspaceId, "Alpha", null).path("id").asLong();
        long secondProject = createProject(token, workspaceId, "Beta", null).path("id").asLong();

        long taskId = createTask(
                columnId(board(firstProject, token), 0),
                token,
                "Stay put",
                "LOW",
                null
        ).path("id").asLong();
        long foreignColumn = columnId(board(secondProject, token), 0);

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/move", taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("columnId", foreignColumn, "orderIndex", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("A task cannot be moved to a column in another project"));
    }

    @Test
    void plainMemberCanContributeTasksButNotCreateProjects() throws Exception {
        String ownerToken = signUp("manager@example.com").path("accessToken").asText();
        String memberToken = signUp("member@example.com").path("accessToken").asText();
        long workspaceId = createWorkspace(ownerToken, "Governance");
        long projectId = createProject(ownerToken, workspaceId, "Charter", null).path("id").asLong();

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "member@example.com"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/projects", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Sneaky"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/projects/{projectId}/columns", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Sneaky column"))))
                .andExpect(status().isForbidden());

        long columnId = columnId(board(projectId, memberToken), 0);
        createTask(columnId, memberToken, "Members can report work", "LOW", null);

        mockMvc.perform(get("/api/v1/projects/{projectId}/board", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[0].tasks[0].title").value("Members can report work"));
    }

    @Test
    void nonMembersAndAnonymousCallersAreRejected() throws Exception {
        String ownerToken = signUp("gatekeeper@example.com").path("accessToken").asText();
        String outsiderToken = signUp("gatekeeper-outsider@example.com").path("accessToken").asText();
        long workspaceId = createWorkspace(ownerToken, "Vault");
        long projectId = createProject(ownerToken, workspaceId, "Secrets", null).path("id").asLong();

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/projects", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/projects/{projectId}/board", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/projects/999999/board")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/projects", workspaceId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void workspaceMembersAreListedForAssigneeSelection() throws Exception {
        String ownerToken = signUp("roster-owner@example.com").path("accessToken").asText();
        String teammateToken = signUp("roster-mate@example.com").path("accessToken").asText();
        String outsiderToken = signUp("roster-outsider@example.com").path("accessToken").asText();
        long workspaceId = createWorkspace(ownerToken, "Roster");

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "roster-mate@example.com"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teammateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].user.email").value("roster-owner@example.com"))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[0].user.password").doesNotExist());

        mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidPayloadsReturnValidationErrors() throws Exception {
        String token = signUp("invalid@example.com").path("accessToken").asText();
        long workspaceId = createWorkspace(token, "Validation");
        long projectId = createProject(token, workspaceId, "Validation", null).path("id").asLong();
        long columnId = columnId(board(projectId, token), 0);

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/projects", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "  "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty());

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/projects", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Bad key", "key", "way-too-long-key"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.key").isNotEmpty());

        mockMvc.perform(post("/api/v1/columns/{columnId}/tasks", columnId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "No priority"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.priority").isNotEmpty());

        mockMvc.perform(patch("/api/v1/tasks/{taskId}/move", 1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("columnId", columnId, "orderIndex", -1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.orderIndex").isNotEmpty());
    }

    private JsonNode signUp(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "firstName", "Flow",
                                "lastName", "User"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private long createWorkspace(String token, String organizationName) throws Exception {
        long organizationId = objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", organizationName))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("id").asLong();

        return objectMapper.readTree(mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/workspaces", organizationId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("name", "Delivery"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("id").asLong();
    }

    private JsonNode createProject(
            String token,
            long workspaceId,
            String name,
            String key
    ) throws Exception {
        Map<String, Object> payload = key == null
                ? Map.of("name", name)
                : Map.of("name", name, "key", key);

        String response = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/projects", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private JsonNode board(long projectId, String token) throws Exception {
        String response = mockMvc.perform(get("/api/v1/projects/{projectId}/board", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private JsonNode createTask(
            long columnId,
            String token,
            String title,
            String priority,
            Long assigneeId
    ) throws Exception {
        Map<String, Object> payload = assigneeId == null
                ? Map.of("title", title, "priority", priority)
                : Map.of("title", title, "priority", priority, "assigneeId", assigneeId);

        String response = mockMvc.perform(post("/api/v1/columns/{columnId}/tasks", columnId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private List<String> columnNames(JsonNode board) {
        return board.path("columns").findValuesAsText("name");
    }

    private long columnId(JsonNode board, int position) {
        return board.path("columns").get(position).path("id").asLong();
    }

    private List<String> tasksInColumn(JsonNode board, long columnId) {
        return column(board, columnId).path("tasks").findValuesAsText("title");
    }

    private List<Integer> orderIndexesInColumn(JsonNode board, long columnId) {
        List<Integer> orderIndexes = new java.util.ArrayList<>();

        for (JsonNode task : column(board, columnId).path("tasks")) {
            orderIndexes.add(task.path("orderIndex").asInt());
        }

        return orderIndexes;
    }

    private JsonNode column(JsonNode board, long columnId) {
        for (JsonNode column : board.path("columns")) {
            if (column.path("id").asLong() == columnId) {
                return column;
            }
        }
        throw new AssertionError("Column not found in board: " + columnId);
    }

    private String json(Map<String, ?> payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }
}
