package com.example.team_workspace.websocket;

import java.util.List;

import com.example.team_workspace.security.CorsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

/**
 * STOMP over WebSocket with a SockJS fallback, for real-time notifications.
 *
 * <p>Note that {@code /user} is configured as the <em>user destination</em> prefix
 * rather than a broker prefix. The broker must only own {@code /topic} and
 * {@code /queue}; registering {@code /user} with the broker would stop
 * {@code /user/queue/notifications} from resolving to the connected principal.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * A single STOMP frame is bounded. Without a limit a client can push an
     * arbitrarily large body, which the broker buffers whole. A notification
     * frame is well under a kilobyte, so 64 KB is already generous.
     */
    private static final int MESSAGE_SIZE_LIMIT_BYTES = 64 * 1024;

    /**
     * Bounds the bytes a single session may have queued for writing. This is the
     * limit that matters for a client which has stopped reading: without it the
     * write buffer grows until the heap is exhausted.
     */
    private static final int SEND_BUFFER_SIZE_LIMIT_BYTES = 256 * 1024;

    /**
     * Fail a stalled session instead of holding its threads: without a send
     * time limit a dead client parks a broker thread indefinitely.
     */
    private static final int SEND_TIME_LIMIT_MILLIS = 10_000;

    /**
     * Rejects half-open sockets that never send CONNECT, so an unreachable
     * client cannot occupy a session forever.
     */
    private static final int TIME_TO_FIRST_MESSAGE_MILLIS = 10_000;

    /**
     * Spring sizes the STOMP channel pools at 2x the available processors with
     * an unbounded queue. That is a poor fit for a notification fan-out: the
     * work per frame is tiny, so the pool only needs to be wide enough to keep
     * busy sessions moving, and the queue must be bounded so a burst cannot pin
     * frames in memory until GC thrashes.
     */
    private static final int CHANNEL_CORE_POOL_SIZE = 2;
    private static final int CHANNEL_MAX_POOL_SIZE = 4;
    private static final int CHANNEL_QUEUE_CAPACITY = 256;
    private static final int CHANNEL_KEEP_ALIVE_SECONDS = 60;

    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;
    private final CorsProperties corsProperties;

    public WebSocketConfig(
            StompJwtChannelInterceptor stompJwtChannelInterceptor,
            CorsProperties corsProperties
    ) {
        this.stompJwtChannelInterceptor = stompJwtChannelInterceptor;
        this.corsProperties = corsProperties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(corsProperties.allowedOrigins().toArray(String[]::new))
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(CHANNEL_CORE_POOL_SIZE)
                .maxPoolSize(CHANNEL_MAX_POOL_SIZE)
                .queueCapacity(CHANNEL_QUEUE_CAPACITY)
                .keepAliveSeconds(CHANNEL_KEEP_ALIVE_SECONDS);
        registration.interceptors(stompJwtChannelInterceptor);
    }

    /**
     * Outbound fan-out is one job per subscriber, so it gets the same bounded
     * pool. Bounding the queue is what stops a slow tab from turning one push
     * into an unbounded backlog.
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(CHANNEL_CORE_POOL_SIZE)
                .maxPoolSize(CHANNEL_MAX_POOL_SIZE)
                .queueCapacity(CHANNEL_QUEUE_CAPACITY)
                .keepAliveSeconds(CHANNEL_KEEP_ALIVE_SECONDS);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(MESSAGE_SIZE_LIMIT_BYTES)
                .setSendBufferSizeLimit(SEND_BUFFER_SIZE_LIMIT_BYTES)
                .setSendTimeLimit(SEND_TIME_LIMIT_MILLIS)
                .setTimeToFirstMessage(TIME_TO_FIRST_MESSAGE_MILLIS);
    }

    /**
     * Without this the STOMP channels only know {@link SimpleMessageConverter},
     * so a JSON body would arrive at {@code @MessageMapping} as a String and fail
     * to bind to {@code MarkNotificationReadRequest}, and the envelope could not
     * be serialised on the way out.
     */
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(new MappingJackson2MessageConverter());
        return true;
    }
}
