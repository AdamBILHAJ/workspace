package com.example.team_workspace.auth.service;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Locale;

import com.example.team_workspace.auth.dto.AuthResponse;
import com.example.team_workspace.auth.dto.SignInRequest;
import com.example.team_workspace.auth.dto.SignUpRequest;
import com.example.team_workspace.auth.exception.EmailAlreadyExistsException;
import com.example.team_workspace.auth.exception.InvalidPasswordException;
import com.example.team_workspace.security.JwtTokenProvider;
import com.example.team_workspace.user.domain.Role;
import com.example.team_workspace.user.domain.User;
import com.example.team_workspace.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        validateBcryptPassword(request.password());
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(
                email,
                passwordHash,
                request.firstName(),
                request.lastName(),
                Role.ROLE_USER
        );

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException();
        }

        return AuthResponse.from(user, jwtTokenProvider.generateToken(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse signIn(SignInRequest request) {
        validateBcryptPassword(request.password());
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        normalizeEmail(request.email()),
                        request.password()
                )
        );

        if (!(authentication.getPrincipal() instanceof User user)) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return AuthResponse.from(user, jwtTokenProvider.generateToken(user));
    }

    private void validateBcryptPassword(String password) {
        if (password.getBytes(UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new InvalidPasswordException();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
