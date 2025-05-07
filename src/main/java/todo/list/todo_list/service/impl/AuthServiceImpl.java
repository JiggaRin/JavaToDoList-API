package todo.list.todo_list.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import todo.list.todo_list.dto.Auth.AuthRequest;
import todo.list.todo_list.dto.Auth.AuthResponse;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.exception.CannotProceedException;
import todo.list.todo_list.security.JwtUtil;
import todo.list.todo_list.service.AuthService;
import todo.list.todo_list.service.RefreshTokenService;
import todo.list.todo_list.service.UserService;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final long LOGIN_ATTEMPT_WINDOW_SECONDS = 60;
    private static final Map<String, List<Long>> LOGIN_ATTEMPTS = new ConcurrentHashMap<>();
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(JwtUtil jwtUtil, RefreshTokenService refreshTokenService, UserService userService, PasswordEncoder passwordEncoder) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthResponse authenticate(AuthRequest authRequest) {
        log.debug("Received authentication request");
        validateAuthRequest(authRequest);

        String username = authRequest.getUsername();
        log.debug("Authenticating user: {}", username);

        if (username.length() < MIN_USERNAME_LENGTH) {
            log.warn("Short username detected: {}", username);
        }

        trackLoginAttempt(username);

        User authenticatedUser = userService.getUserByUsername(username);
        validatePassword(authRequest.getPassword(), authenticatedUser.getPassword());

        List<String> userRoles = extractRoles(authenticatedUser);
        if (userRoles.isEmpty()) {
            log.warn("No roles assigned to user: {}", username);
            throw new CannotProceedException("User has no assigned roles");
        }

        String accessToken = jwtUtil.generateAccessToken(username, userRoles);
        String refreshToken = refreshTokenService.createRefreshToken(username).getRefreshToken();
        log.info("Successfully authenticated user: {}", username);

        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.debug("Refreshing token: {}", refreshToken != null ? refreshToken.substring(0, Math.min(10, refreshToken.length())) + "..." : null);
        validateRefreshToken(refreshToken);

        String username = jwtUtil.extractUsername(refreshToken);
        if (username.length() < MIN_USERNAME_LENGTH) {
            log.warn("Short username detected during token refresh: {}", username);
        }

        User authenticatedUser = userService.getUserByUsername(username);
        List<String> userRoles = extractRoles(authenticatedUser);
        String newAccessToken = jwtUtil.generateAccessToken(username, userRoles);
        log.info("Successfully refreshed access token for user: {}", username);

        return new AuthResponse(newAccessToken, refreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        log.debug("Logging out with token: {}", refreshToken != null ? refreshToken.substring(0, Math.min(10, refreshToken.length())) + "..." : null);
        validateRefreshToken(refreshToken);
        
        String username = jwtUtil.extractUsername(refreshToken);
        refreshTokenService.deleteByUsername(username);
        log.info("Successfully logged out user: {}", username);
    }

    private void validateAuthRequest(AuthRequest authRequest) {
        if (authRequest == null || authRequest.getUsername() == null || authRequest.getPassword() == null) {
            throw new IllegalArgumentException("Authentication request or credentials cannot be null");
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new CannotProceedException("Invalid password");
        }
    }

    private void validateRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token cannot be null");
        }
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
    }

    private List<String> extractRoles(User user) {
        if (user == null || user.getAuthorities() == null) {
            return Collections.emptyList();
        }
        return user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList());
    }

    private void trackLoginAttempt(String username) {
        long currentTime = Instant.now().getEpochSecond();
        List<Long> attempts = LOGIN_ATTEMPTS.computeIfAbsent(username, k -> new ArrayList<>());

        attempts.removeIf(timestamp -> currentTime - timestamp > LOGIN_ATTEMPT_WINDOW_SECONDS);
        
        attempts.add(currentTime);
        
        if (attempts.size() > MAX_LOGIN_ATTEMPTS) {
            log.warn("Frequent login attempts detected for user: {}, attempts: {}", username, attempts.size());
        }
    }
}