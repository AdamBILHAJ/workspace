package com.example.team_workspace.websocket;

import java.security.Principal;
import java.util.Set;

import com.example.team_workspace.user.domain.User;
import com.example.team_workspace.user.service.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.example.team_workspace.security.JwtTokenProvider;

/**
 * Authenticates STOMP CONNECT frames with the same JWT used by the REST API.
 *
 * <p>The handshake itself is unauthenticated (it is permitted in
 * {@code SecurityConfig}); the token is validated here on CONNECT and the
 * resulting Principal is stored on the session, which is what makes
 * {@code /user/queue/**} destinations and {@code @MessageMapping} handlers able
 * to identify the caller.
 */
@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StompJwtChannelInterceptor.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Frames that can change server state. SUBSCRIBE is deliberately excluded: a
     * subscription without a Principal resolves to no user destination and is
     * therefore inert, whereas SEND can mutate another user's rows.
     */
    private static final Set<StompCommand> STATE_CHANGING = Set.of(
            StompCommand.SEND,
            StompCommand.MESSAGE,
            StompCommand.BEGIN,
            StompCommand.COMMIT
    );

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public StompJwtChannelInterceptor(
            JwtTokenProvider jwtTokenProvider,
            CustomUserDetailsService userDetailsService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            try {
                accessor.setUser(authenticate(accessor));
            } catch (MessageDeliveryException exception) {
                LOGGER.warn("Rejected STOMP CONNECT: {}", exception.getMessage());
                throw exception;
            }
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        // Spring restores the Principal from the session for every frame after
        // CONNECT, so this is normally already populated. It is asserted rather
        // than assumed so an unauthenticated frame can never reach a handler.
        if (accessor.getUser() == null && STATE_CHANGING.contains(accessor.getCommand())) {
            throw new MessageDeliveryException("STOMP session is not authenticated");
        }

        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

        if (authorization == null
                || authorization.length() <= BEARER_PREFIX.length()
                || !authorization.regionMatches(
                        true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new MessageDeliveryException("Missing Bearer token on STOMP CONNECT");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        String username;

        try {
            username = jwtTokenProvider.extractUsername(token);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new MessageDeliveryException("Invalid token on STOMP CONNECT");
        }

        UserDetails userDetails;

        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException exception) {
            throw new MessageDeliveryException("Unknown user on STOMP CONNECT");
        }

        if (!jwtTokenProvider.isTokenValid(token, userDetails)) {
            throw new MessageDeliveryException("Expired or invalid token on STOMP CONNECT");
        }

        if (!(userDetails instanceof User user)) {
            throw new MessageDeliveryException("Principal is not a known user");
        }

        // UserDetails is not a Principal, so the entity is wrapped. The
        // authentication's getName() resolves to the email, which is the key
        // used by /user/** destination resolution.
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    /**
     * Unwraps the {@link User} entity that {@link #authenticate} placed in the
     * session Principal.
     */
    public static User resolveUser(Principal principal) {
        if (principal == null) {
            throw new MessageDeliveryException("Unauthenticated STOMP session");
        }

        if (principal instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof User user) {
            return user;
        }

        throw new MessageDeliveryException("Unsupported STOMP principal type");
    }
}
