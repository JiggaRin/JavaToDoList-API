package todo.list.todo_list.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import todo.list.todo_list.dto.Auth.AuthRequest;
import todo.list.todo_list.dto.Auth.AuthResponse;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.exception.BadCredentialsException;
import todo.list.todo_list.security.JwtUtil;
import todo.list.todo_list.service.AuthService;
import todo.list.todo_list.service.RefreshTokenService;
import todo.list.todo_list.service.UserService;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
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
    public AuthResponse authenticate(AuthRequest authRequest) {
        log.debug("Received authentication request");
        validateAuthRequest(authRequest);

        String username = authRequest.getUsername();
        log.debug("Authenticating user: {}", username);
        User authenticatedUser = userService.getUserByUsername(username);

        validatePassword(authRequest.getPassword(), authenticatedUser.getPassword());
        List<String> userRoles = extractRoles(authenticatedUser);
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
        if (authRequest == null) {
            throw new IllegalArgumentException("Auth request cannot be null");
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BadCredentialsException("Invalid username or password");
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
}