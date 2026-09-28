package com.example.team_workspace.activity.socket;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

/**
 * A deliberately small STOMP-over-WebSocket client for tests.
 *
 * <p>Spring's own {@code WebSocketStompClient} cannot be used to assert the
 * notification stream. Its {@code DefaultStompSession} routes an incoming MESSAGE
 * to a subscription by comparing the frame {@code destination} header against the
 * destination the client subscribed with. A user destination is rewritten
 * server-side to {@code /queue/notifications-user{sessionId}}, so a Java client
 * subscribed to {@code /user/queue/notifications} silently drops every frame.
 *
 * <p>The real browser client is unaffected: {@code @stomp/stompjs} dispatches on
 * the {@code subscription} id header that the broker echoes back, so the
 * destination may be rewritten. This client therefore does the same, which keeps
 * the test faithful to what the frontend actually observes.
 *
 * <p>Every instance is {@link AutoCloseable} and releases its subscriptions,
 * queues and session. Leaking a session is the failure mode that actually bites
 * here: the server keeps the session, its subscription and its buffered outbound
 * frames alive, and each one holds a reference into the shared application
 * context for the rest of the run.
 */
final class RawStompTestClient implements AutoCloseable {

    /**
     * Bounded so a broken broker fails the test quickly instead of parking a
     * thread for the full 30s Surefire timeout and again on every later test.
     */
    private static final long CONNECT_TIMEOUT_SECONDS = 3;

    private static final String SUBPROTOCOL = "v12.stomp";
    private static final char NULL = '\0';

    /**
     * One container for the whole JVM. {@code ContainerProvider} may hand back a
     * fresh implementation per call, and each one owns a thread pool; sharing a
     * single container keeps the client-side thread count flat as the number of
     * connections grows.
     */
    private static final WebSocketContainer CONTAINER = createContainer();

    /** Subscription ids must be unique per session, and sessions are per test. */
    private static final AtomicInteger SUBSCRIPTION_SEQUENCE = new AtomicInteger();

    private final Map<String, Consumer> subscriptions = new ConcurrentHashMap<>();
    private final CountDownLatch connected = new CountDownLatch(1);

    private Session session;
    private MessageHandler.Whole<String> messageHandler;
    private volatile boolean connectRejected;
    private volatile String rejectionMessage;
    private volatile boolean closed;

    private RawStompTestClient() {
    }

    private static WebSocketContainer createContainer() {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        // A session left open by a failed test would otherwise hold its buffers
        // indefinitely; this is the backstop behind close().
        container.setDefaultMaxSessionIdleTimeout(TimeUnit.SECONDS.toMillis(30));
        container.setDefaultMaxTextMessageBufferSize(64 * 1024);
        return container;
    }

    private void attach(Session session) {
        this.session = session;
        this.messageHandler = new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                onText(message);
            }
        };
        session.addMessageHandler(messageHandler);
    }

    /**
     * Opens the WebSocket and completes a STOMP CONNECT.
     *
     * @throws AssertionFailure if the server answers CONNECT with ERROR
     */
    static RawStompTestClient connect(String url, String bearerToken) throws Exception {
        ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                .preferredSubprotocols(List.of(SUBPROTOCOL))
                .build();

        RawStompTestClient client = new RawStompTestClient();
        client.session = CONTAINER.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig config) {
                client.attach(session);
            }
        }, config, URI.create(url));

        try {
            client.sendFrame("CONNECT\naccept-version:1.2\nhost:localhost\n"
                    + "Authorization:Bearer " + bearerToken + "\n\n");

            if (!client.connected.await(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionFailure(
                        "no CONNECTED frame within " + CONNECT_TIMEOUT_SECONDS + "s");
            }

            if (client.connectRejected) {
                throw new AssertionFailure("CONNECT rejected: " + client.rejectionMessage);
            }

            return client;
        } catch (Exception | AssertionError failure) {
            // Never hand back a half-open client: the caller's teardown list
            // only ever sees successfully connected clients, so this is the one
            // place the session can be released.
            client.close();
            throw failure;
        }
    }

    /**
     * Subscribes and returns the queue that receives payloads for that
     * subscription. Frames are routed by subscription id, like the browser.
     */
    BlockingQueue<String> subscribe(String destination) throws Exception {
        BlockingQueue<String> payloads = new LinkedBlockingQueue<>(16);
        String id = "sub-" + SUBSCRIPTION_SEQUENCE.incrementAndGet();

        subscriptions.put(id, frame -> {
            if ("MESSAGE".equals(frame.command())) {
                // offer() with a bound: a test that stops reading must not let
                // the queue grow without limit.
                payloads.offer(frame.body());
            }
        });

        sendFrame("SUBSCRIBE\nid:" + id + "\ndestination:" + destination + "\nack:auto\n\n");
        return payloads;
    }

    void send(String destination, String body) throws Exception {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        sendFrame("SEND\ndestination:" + destination
                + "\ncontent-type:application/json;charset=utf-8"
                + "\ncontent-length:" + bytes.length
                + "\n\n" + body);
    }

    /**
     * Idempotent, and safe to call on a client whose connect failed. Releases
     * the subscription map (and with it every payload queue) before closing the
     * session, so a closed client retains no test frames.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        subscriptions.clear();

        Session current = session;
        if (current == null) {
            return;
        }

        try {
            if (messageHandler != null) {
                current.removeMessageHandler(messageHandler);
            }
        } catch (RuntimeException ignored) {
            // A session closed by the peer may reject handler removal
        }

        try {
            if (current.isOpen()) {
                current.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "test over"));
            }
        } catch (IOException ignored) {
            // nothing useful to do while tearing a test client down
        }
    }

    private void sendFrame(String frame) throws IOException {
        if (closed || !session.isOpen()) {
            throw new IOException("STOMP session is not open");
        }
        session.getBasicRemote().sendText(frame + NULL);
    }

    private void onText(String text) {
        if (closed) {
            return;
        }

        Frame frame = Frame.parse(text);
        if (frame == null) {
            return;
        }

        switch (frame.command()) {
            case "CONNECTED" -> connected.countDown();
            case "ERROR" -> {
                connectRejected = true;
                rejectionMessage = frame.header("message");
                connected.countDown();
            }
            case "MESSAGE" -> {
                Consumer handler = subscriptions.get(frame.header("subscription"));
                if (handler != null) {
                    handler.accept(frame);
                }
            }
            default -> {
                // HEARTBEAT, RECEIPT and anything else are not under test
            }
        }
    }

    @FunctionalInterface
    private interface Consumer {
        void accept(Frame frame);
    }

    /** Raised instead of a JUnit assertion so connect() can signal a rejected CONNECT. */
    static final class AssertionFailure extends RuntimeException {
        AssertionFailure(String message) {
            super(message);
        }
    }

    /**
     * Just enough STOMP frame parsing: the first line is the command, the lines up
     * to the blank line are headers, and the remainder up to the NULL octet is
     * the body.
     */
    record Frame(String command, Map<String, String> headers, String body) {

        String header(String name) {
            return headers.get(name.toLowerCase(java.util.Locale.ROOT));
        }

        static Frame parse(String text) {
            int end = text.indexOf(NULL);
            String content = end >= 0 ? text.substring(0, end) : text;

            String[] lines = content.split("\n");
            if (lines.length == 0 || lines[0].isBlank()) {
                return null;
            }

            Map<String, String> headers = new ConcurrentHashMap<>();
            int index = 1;
            while (index < lines.length && !lines[index].isEmpty()) {
                int colon = lines[index].indexOf(':');
                if (colon > 0) {
                    headers.put(
                            lines[index].substring(0, colon).trim().toLowerCase(java.util.Locale.ROOT),
                            lines[index].substring(colon + 1).trim()
                    );
                }
                index++;
            }

            String body = index + 1 <= lines.length
                    ? String.join("\n", java.util.Arrays.copyOfRange(lines, Math.min(index, lines.length), lines.length))
                    : "";

            return new Frame(lines[0].trim(), headers, body);
        }
    }
}
