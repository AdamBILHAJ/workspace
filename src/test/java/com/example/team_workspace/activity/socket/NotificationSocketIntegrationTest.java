package com.example.team_workspace.activity.socket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.example.team_workspace.support.DatabaseTruncator;
import com.example.team_workspace.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of the STOMP notification channel: a real WebSocket
 * handshake, a real JWT-authenticated CONNECT, and real commits.
 *
 * <p>Deliberately not @Transactional, for the same reason as
 * TaskEngagementIntegrationTest: the listeners that push these frames run on
 * AFTER_COMMIT, which never fires inside a rolled-back test transaction.
 *
 * <p>Every test is expected to cost the same small amount of memory as the last:
 * each one disconnects its WebSocket sessions in {@code @AfterEach} and wipes
 * the shared database on both sides of the test. See {@link IntegrationTest} for
 * why a single application context is shared across the whole suite.
 */
@IntegrationTest
class NotificationSocketIntegrationTest {

    private static final String PASSWORD = "StrongPassword123!";
    private static final String NOTIFICATIONS = "/user/queue/notifications";
    private static final String ERRORS = "/user/queue/notifications/errors";

    /**
     * Upper bound for every wait in this class. A local broker answers in
     * milliseconds, so three seconds is generous; a longer bound only delays the
     * moment a real failure is reported and keeps threads parked while the
     * remaining tests run.
     */
    private static final long FRAME_TIMEOUT_SECONDS = 3;

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<RawStompTestClient> clients = new ArrayList<>();

    private String ownerToken;
    private String mateToken;
    private long mateId;
    private long columnId;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseTruncator.truncate(jdbcTemplate);

        ownerToken = signUp("owner@socket.io", "Owner");
        mateToken = signUp("mate@socket.io", "Mate");
        mateId = userId("mate@socket.io");
        columnId = bootstrapProject();
    }

    /**
     * Closes every session this test opened and then empties the tables. Order
     * matters: a session that is still connected would otherwise receive frames
     * for data being deleted underneath it.
     */
    @AfterEach
    void tearDown() {
        clients.forEach(RawStompTestClient::close);
        clients.clear();
        DatabaseTruncator.truncate(jdbcTemplate);
    }

    @Test
    void assigningATaskPushesToTheAssigneeInRealTime() throws Exception {
        BlockingQueue<String> inbox = subscribe(mateToken);

        createTask("Ship the release", mateId);

        JsonNode event = awaitEvent(inbox);
        assertThat(event.path("type").asText()).isEqualTo("NEW_NOTIFICATION");
        assertThat(event.path("unreadCount").asLong()).isEqualTo(1);
        assertThat(event.path("payload").path("id").asLong()).isPositive();
        assertThat(event.path("payload").path("title").asText()).isEqualTo("New task assigned to you");
        assertThat(event.path("payload").path("createdAt").asText()).isNotBlank();
        assertThat(event.path("payload").path("targetUrl").asText()).contains("?task=");
    }

    @Test
    void aCommentPushesToTheAssignee() throws Exception {
        long taskId = createTask("Needs review", mateId);
        BlockingQueue<String> inbox = subscribe(mateToken);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "Please take a look"))))
                .andExpect(status().isCreated());

        JsonNode event = awaitEvent(inbox);
        assertThat(event.path("type").asText()).isEqualTo("NEW_NOTIFICATION");
        assertThat(event.path("payload").path("title").asText()).isEqualTo("New comment on your task");
    }

    @Test
    void notificationsAreNotLeakedToAnotherUser() throws Exception {
        BlockingQueue<String> mateInbox = subscribe(mateToken);
        RawStompTestClient owner = connect(ownerToken);
        BlockingQueue<String> ownerInbox = owner.subscribe(NOTIFICATIONS);

        // A task with no assignee notifies nobody, so neither session may see a frame.
        createTask("Private to owner", null);

        assertThat(mateInbox.poll(2, TimeUnit.SECONDS)).isNull();
        assertThat(ownerInbox.poll(2, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void aNotificationIsNotPushedToAThirdParty() throws Exception {
        BlockingQueue<String> mateInbox = subscribe(mateToken);
        RawStompTestClient owner = connect(ownerToken);
        BlockingQueue<String> ownerInbox = owner.subscribe(NOTIFICATIONS);

        createTask("Assigned to the mate", mateId);

        JsonNode event = awaitEvent(mateInbox);
        assertThat(event.path("payload").path("title").asText()).isEqualTo("New task assigned to you");
        assertThat(ownerInbox.poll(2, TimeUnit.SECONDS)).as("the actor must not be notified").isNull();
    }

    @Test
    void markingReadOverTheSocketUpdatesTheDatabaseAndTheBadge() throws Exception {
        RawStompTestClient client = connect(mateToken);
        BlockingQueue<String> inbox = client.subscribe(NOTIFICATIONS);

        // Subscribed before the task exists: the push is a live broadcast, which is
        // exactly why the bell also loads over REST on mount.
        createTask("Read me", mateId);
        awaitEvent(inbox);

        assertThat(unreadRowsForMate()).isEqualTo(1);
        long notificationId = unreadIdForMate();

        client.send("/app/notifications.read", "{\"id\":" + notificationId + "}");

        JsonNode event = awaitEvent(inbox);
        assertThat(event.path("type").asText()).isEqualTo("NOTIFICATION_READ");
        assertThat(event.path("payload").path("id").asLong()).isEqualTo(notificationId);
        assertThat(event.path("payload").path("isRead").asBoolean()).isTrue();
        assertThat(event.path("unreadCount").asLong()).isZero();
        assertThat(unreadRowsForMate()).isZero();
    }

    @Test
    void markingAllReadOverTheSocketClearsEveryRowAndZeroesTheBadge() throws Exception {
        RawStompTestClient firstTab = connect(mateToken);
        BlockingQueue<String> firstInbox = firstTab.subscribe(NOTIFICATIONS);

        createTask("First", mateId);
        createTask("Second", mateId);
        awaitEvent(firstInbox);
        awaitEvent(firstInbox);

        RawStompTestClient secondTab = connect(mateToken);
        BlockingQueue<String> secondInbox = secondTab.subscribe(NOTIFICATIONS);

        assertThat(unreadRowsForMate()).isEqualTo(2);

        firstTab.send("/app/notifications.readAll", "");

        JsonNode event = awaitEvent(firstInbox);
        assertThat(event.path("type").asText()).isEqualTo("ALL_READ");
        assertThat(event.path("unreadCount").asLong()).isZero();
        assertThat(event.path("payload").asInt()).isEqualTo(2);
        assertThat(unreadRowsForMate()).isZero();

        // The same user's other tab must converge on zero as well.
        JsonNode mirrored = awaitEvent(secondInbox);
        assertThat(mirrored.path("type").asText()).isEqualTo("ALL_READ");
        assertThat(mirrored.path("unreadCount").asLong()).isZero();
    }

    @Test
    void aNotificationCreatedBeforeSubscribingIsNotReplayed() throws Exception {
        createTask("Created while offline", mateId);

        RawStompTestClient client = connect(mateToken);
        BlockingQueue<String> inbox = client.subscribe(NOTIFICATIONS);

        // No replay: the bell recovers missed notifications from REST, not the socket.
        assertThat(inbox.poll(2, TimeUnit.SECONDS)).isNull();
        assertThat(unreadRowsForMate()).as("but the unread row is still there for REST").isEqualTo(1);
    }

    @Test
    void connectingWithoutABearerTokenIsRejected() {
        assertThatThrownBy(() -> connect(null))
                .isInstanceOf(RawStompTestClient.AssertionFailure.class)
                .hasMessageContaining("CONNECT rejected");
    }

    @Test
    void connectingWithAGarbageTokenIsRejected() {
        assertThatThrownBy(() -> RawStompTestClient.connect(wsUrl(), "not-a-real-jwt"))
                .isInstanceOf(RawStompTestClient.AssertionFailure.class)
                .hasMessageContaining("CONNECT rejected");
    }

    @Test
    void anotherUsersNotificationCannotBeMarkedRead() throws Exception {
        RawStompTestClient mate = connect(mateToken);
        BlockingQueue<String> mateInbox = mate.subscribe(NOTIFICATIONS);
        RawStompTestClient owner = connect(ownerToken);
        BlockingQueue<String> ownerErrors = owner.subscribe(ERRORS);

        createTask("Assigned to the mate", mateId);
        awaitEvent(mateInbox);

        long notificationId = unreadIdForMate();
        owner.send("/app/notifications.read", "{\"id\":" + notificationId + "}");

        String error = ownerErrors.poll(FRAME_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(error).as("the caller must be told the request failed").isNotNull();
        assertThat(objectMapper.readTree(error).path("message").asText()).isNotBlank();

        // The owner must not be able to flip the mate's row.
        assertThat(unreadRowsForMate()).as("ownership must be enforced over STOMP").isEqualTo(1);
        assertThat(mateInbox.poll(1, TimeUnit.SECONDS)).as("the mate must see no state change").isNull();
    }

    // --- harness ---------------------------------------------------------

    private RawStompTestClient connect(String token) throws Exception {
        RawStompTestClient client = RawStompTestClient.connect(wsUrl(), token);
        clients.add(client);
        return client;
    }

    private BlockingQueue<String> subscribe(String token) throws Exception {
        return connect(token).subscribe(NOTIFICATIONS);
    }

    private JsonNode awaitEvent(BlockingQueue<String> inbox) throws Exception {
        String payload = inbox.poll(FRAME_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(payload).as("expected a STOMP frame within %ds", FRAME_TIMEOUT_SECONDS).isNotNull();
        return objectMapper.readTree(payload);
    }

    private long unreadRowsForMate() {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from notifications where recipient_id = ? and is_read = false",
                Long.class,
                mateId
        );
        return count == null ? 0 : count;
    }

    private long unreadIdForMate() {
        Long id = jdbcTemplate.queryForObject(
                "select id from notifications where recipient_id = ? order by id desc limit 1",
                Long.class,
                mateId
        );
        assertThat(id).isNotNull();
        return id;
    }

    // --- REST scaffolding ------------------------------------------------

    private long bootstrapProject() throws Exception {
        long organizationId = objectMapper.readTree(mockMvc.perform(post("/api/v1/organizations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Socket Org"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asLong();

        long workspaceId = objectMapper.readTree(mockMvc.perform(
                        post("/api/v1/organizations/{organizationId}/workspaces", organizationId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("name", "Realtime"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asLong();

        long projectId = objectMapper.readTree(mockMvc.perform(
                        post("/api/v1/workspaces/{workspaceId}/projects", workspaceId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("name", "Platform"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "mate@socket.io"))))
                .andExpect(status().isCreated());

        String board = mockMvc.perform(get("/api/v1/projects/{projectId}/board", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(board).path("columns").get(0).path("id").asLong();
    }

    private long createTask(String title, Long assigneeId) throws Exception {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("title", title);
        payload.put("priority", "MEDIUM");
        if (assigneeId != null) {
            payload.put("assigneeId", assigneeId);
        }

        return objectMapper.readTree(mockMvc.perform(post("/api/v1/columns/{columnId}/tasks", columnId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asLong();
    }

    private String signUp(String email, String firstName) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "firstName", firstName,
                                "lastName", "User"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("accessToken").asText();
    }

    private long userId(String email) {
        Long id = jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
        assertThat(id).isNotNull();
        return id;
    }

    private String wsUrl() {
        return "ws://localhost:" + port + "/ws/websocket";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Map<String, ?> payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }
}
