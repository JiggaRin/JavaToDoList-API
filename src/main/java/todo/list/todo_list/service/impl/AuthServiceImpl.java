package todo.list.todo_list.service.impl;

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
        if (authRequest == null) {
            throw new IllegalArgumentException("Auth request cannot be null");
        }
        User user = this.userService.getUserByUsername(authRequest.getUsername());

        if (!this.passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        List<String> roles = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList());
        String accessToken = this.jwtUtil.generateAccessToken(authRequest.getUsername(), roles);
        String refreshToken = this.refreshTokenService.createRefreshToken(authRequest.getUsername()).getRefreshToken();

        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || !this.jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String username = this.jwtUtil.extractUsername(refreshToken);
        User user = this.userService.getUserByUsername(username);
        List<String> roles = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList());

        String newAccessToken = this.jwtUtil.generateAccessToken(username, roles);
        log.info("Refreshed access token for user {}", username);
        return new AuthResponse(newAccessToken, refreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || !this.jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        String username = this.jwtUtil.extractUsername(refreshToken);
        this.refreshTokenService.deleteByUsername(username);
        log.info("User {} logged out successfully", username);
    }
}
