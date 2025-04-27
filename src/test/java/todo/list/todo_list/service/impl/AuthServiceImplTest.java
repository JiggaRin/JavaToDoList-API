package todo.list.todo_list.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import todo.list.todo_list.dto.Auth.AuthRequest;
import todo.list.todo_list.dto.Auth.AuthResponse;
import todo.list.todo_list.entity.RefreshToken;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.exception.BadCredentialsException;
import todo.list.todo_list.exception.ResourceNotFoundException;
import todo.list.todo_list.model.Role;
import todo.list.todo_list.security.JwtUtil;
import todo.list.todo_list.service.RefreshTokenService;
import todo.list.todo_list.service.UserService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private final String username = "testuser";

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private AuthRequest setupAuthRequest(String username, String password) {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);

        return request;
    }

    private User setupUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(Role.USER);

        return user;
    }

    @Test
    @DisplayName("Authenticate with valid credentials returns tokens")
    void authenticate_successfulAuthentication() {
        AuthRequest request = this.setupAuthRequest(this.username, "Password123!");

        User user = this.setupUser(this.username, "encodedPassword");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRefreshToken("refresh-token-value");

        when(this.userService.getUserByUsername(this.username)).thenReturn(user);
        when(this.passwordEncoder.matches("Password123!", "encodedPassword")).thenReturn(true);
        when(this.jwtUtil.generateAccessToken(this.username, List.of("ROLE_USER"))).thenReturn("access-token-value");
        when(this.refreshTokenService.createRefreshToken(this.username)).thenReturn(refreshToken);

        AuthResponse response = this.authService.authenticate(request);

        assertNotNull(response);
        assertEquals("access-token-value", response.getAccessToken());
        assertEquals("refresh-token-value", response.getRefreshToken());
        verify(this.userService).getUserByUsername(this.username);
        verify(this.passwordEncoder).matches("Password123!", "encodedPassword");
        verify(this.jwtUtil).generateAccessToken(this.username, List.of("ROLE_USER"));
        verify(this.refreshTokenService).createRefreshToken(this.username);
    }

    @Test
    @DisplayName("Authenticate with unknown user throws UsernameNotFoundException")
    void authenticate_userNotFound_throwsException() {
        AuthRequest request = this.setupAuthRequest("unknownuser", "Password123!");

        when(this.userService.getUserByUsername("unknownuser"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> this.authService.authenticate(request)
        );
        assertEquals("User not found", exception.getMessage());
        verify(this.userService).getUserByUsername("unknownuser");
        verify(this.jwtUtil, never()).generateAccessToken(anyString(), anyList());
        verify(this.refreshTokenService, never()).createRefreshToken(anyString());
    }

    @Test
    @DisplayName("Authenticate with invalid password throws BadCredentialsException")
    void authenticate_invalidPassword_throwsException() {
        AuthRequest request = this.setupAuthRequest(this.username, "WrongPassword");

        User user = this.setupUser(this.username, "encodedPassword");

        when(this.userService.getUserByUsername(this.username)).thenReturn(user);
        when(this.passwordEncoder.matches("WrongPassword", "encodedPassword")).thenReturn(false);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> this.authService.authenticate(request)
        );
        assertEquals("Invalid username or password", exception.getMessage());
        verify(this.userService).getUserByUsername(this.username);
        verify(this.passwordEncoder).matches("WrongPassword", "encodedPassword");
        verify(this.jwtUtil, never()).generateAccessToken(anyString(), anyList());
        verify(this.refreshTokenService, never()).createRefreshToken(anyString());
    }

    @Test
    @DisplayName("Authenticate but Auth request in NULL throws IllegalArgumentException")
    void authenticate_nullRequest_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.authService.authenticate(null)
        );
        assertEquals("Auth request cannot be null", exception.getMessage());

        verify(this.userService, never()).getUserByUsername(anyString());
        verify(this.passwordEncoder, never()).matches(anyString(), anyString());
        verify(this.jwtUtil, never()).generateAccessToken(anyString(), any());
        verify(this.refreshTokenService, never()).createRefreshToken(anyString());
    }

    @Test
    @DisplayName("Refresh token with valid token returns new access token")
    void refreshToken_successfulRefreshing() {
        String refreshToken = "valid-refresh-token";
        RefreshToken token = new RefreshToken();
        token.setRefreshToken(refreshToken);

        when(this.jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(this.jwtUtil.extractUsername(refreshToken)).thenReturn(this.username);

        User user = this.setupUser(this.username, null);

        when(this.userService.getUserByUsername(this.username)).thenReturn(user);
        when(this.jwtUtil.generateAccessToken(this.username, List.of("ROLE_USER"))).thenReturn("new-access-token");

        AuthResponse response = this.authService.refreshToken(refreshToken);
        assertNotNull(response);

        InOrder inOrder = inOrder(this.jwtUtil, this.userService);
        inOrder.verify(this.jwtUtil).validateToken(refreshToken);
        inOrder.verify(this.jwtUtil).extractUsername(refreshToken);
        inOrder.verify(this.userService).getUserByUsername(user.getUsername());
        inOrder.verify(this.jwtUtil).generateAccessToken(user.getUsername(), List.of("ROLE_USER"));

        verify(this.jwtUtil).validateToken(refreshToken);
        verify(this.jwtUtil).extractUsername(refreshToken);
        verify(this.userService).getUserByUsername(user.getUsername());
        verify(this.jwtUtil).generateAccessToken(user.getUsername(), List.of("ROLE_USER"));
    }

    @Test
    @DisplayName("Refresh token with invalid token throws IllegalArgumentException")
    void refreshToken_invalidRefreshToken_throwsException() {
        String refreshToken = "invalid-refresh-token";

        when(this.jwtUtil.validateToken(refreshToken))
                .thenThrow(new IllegalArgumentException("Invalid or expired refresh token"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.authService.refreshToken(refreshToken)
        );

        assertEquals("Invalid or expired refresh token", exception.getMessage());

        verify(this.jwtUtil).validateToken(refreshToken);
        verify(this.jwtUtil, never()).extractUsername(anyString());
        verify(this.userService, never()).getUserByUsername(anyString());
        verify(this.jwtUtil, never()).generateAccessToken(anyString(), List.of(anyString()));
    }

    @Test
    @DisplayName("Refresh token with valid token but missing user throws ResourceNotFoundException")
    void refreshToken_userNotFound_throwsException() {
        String refreshToken = "valid-refresh-token";
        RefreshToken token = new RefreshToken();
        token.setRefreshToken(refreshToken);

        when(this.jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(this.jwtUtil.extractUsername(refreshToken)).thenReturn(this.username);

        when(this.userService.getUserByUsername(this.username))
                .thenThrow(new ResourceNotFoundException("User not found with username: " + this.username));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> this.authService.refreshToken(refreshToken)
        );

        assertEquals("User not found with username: " + this.username, exception.getMessage());

        verify(this.jwtUtil).validateToken(refreshToken);
        verify(this.jwtUtil).extractUsername(refreshToken);
        verify(this.userService).getUserByUsername(this.username);
        verify(this.jwtUtil, never()).generateAccessToken(anyString(), List.of(anyString()));
    }

    @Test
    @DisplayName("Logout with valid refresh token deletes token by username")
    void logout_deletesRefreshTokenByUsername() {
        String refreshToken = "valid-refresh-token";

        when(this.jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(this.jwtUtil.extractUsername(refreshToken)).thenReturn(this.username);

        doNothing().when(this.refreshTokenService).deleteByUsername(this.username);

        this.authService.logout(refreshToken);

        InOrder inOrder = inOrder(this.jwtUtil, this.refreshTokenService);
        inOrder.verify(this.jwtUtil).validateToken(refreshToken);
        inOrder.verify(this.jwtUtil).extractUsername(refreshToken);
        inOrder.verify(this.refreshTokenService).deleteByUsername(this.username);

        verify(this.jwtUtil).extractUsername(refreshToken);
        verify(this.refreshTokenService).deleteByUsername(this.username);
    }

    @Test
    @DisplayName("Logout with invalid refresh token throws IllegalArgumentException")
    void logout_invalidToken_throwsException() {
        String refreshToken = "invalid-refresh-token";

        when(this.jwtUtil.validateToken(refreshToken)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.authService.logout(refreshToken)
        );
        assertEquals("Invalid refresh token", exception.getMessage());
        verify(this.jwtUtil).validateToken(refreshToken);
        verify(this.jwtUtil, never()).extractUsername(refreshToken);
        verify(this.refreshTokenService, never()).deleteByUsername(anyString());
    }
}
