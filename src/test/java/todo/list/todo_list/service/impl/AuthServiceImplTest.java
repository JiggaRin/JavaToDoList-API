package todo.list.todo_list.service.impl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import todo.list.todo_list.dto.Auth.AuthRequest;
import todo.list.todo_list.dto.Auth.AuthResponse;
import todo.list.todo_list.entity.RefreshToken;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.exception.CannotProceedException;
import todo.list.todo_list.exception.ResourceNotFoundException;
import todo.list.todo_list.model.Role;
import todo.list.todo_list.security.JwtUtil;
import todo.list.todo_list.service.RefreshTokenService;
import todo.list.todo_list.service.UserService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "Password123!";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String ACCESS_TOKEN = "access-token-value";
    private static final String REFRESH_TOKEN = "refresh-token-value";
    private static final String ROLE_USER = "ROLE_USER";

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

    private User defaultUser;

    @BeforeEach
    @SuppressWarnings("unused")
    void setup() {
        defaultUser = new User(USERNAME, ENCODED_PASSWORD, Role.USER);
    }

    private AuthRequest setupAuthRequest(String username, String password) {
        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private void setupSuccessfulAuthMocks(AuthRequest request, User user, RefreshToken refreshToken) {
        when(userService.getUserByUsername(user.getUsername())).thenReturn(user);
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtUtil.generateAccessToken(user.getUsername(), List.of(ROLE_USER))).thenReturn(ACCESS_TOKEN);
        when(refreshTokenService.createRefreshToken(user.getUsername())).thenReturn(refreshToken);
    }

    private void setupSuccessfulRefreshMocks(String refreshToken, User user) {
        RefreshToken token = new RefreshToken();
        token.setRefreshToken(refreshToken);
        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn(user.getUsername());
        when(userService.getUserByUsername(user.getUsername())).thenReturn(user);
        when(jwtUtil.generateAccessToken(user.getUsername(), List.of(ROLE_USER))).thenReturn(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("Authenticate with valid credentials returns tokens")
    void authenticate_successfulAuthentication() {
        AuthRequest request = this.setupAuthRequest(USERNAME, PASSWORD);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRefreshToken(REFRESH_TOKEN);
        this.setupSuccessfulAuthMocks(request, defaultUser, refreshToken);

        AuthResponse response = authService.authenticate(request);

        assertNotNull(response);
        assertEquals(ACCESS_TOKEN, response.getAccessToken());
        assertEquals(REFRESH_TOKEN, response.getRefreshToken());
        verify(userService).getUserByUsername(USERNAME);
        verify(passwordEncoder).matches(PASSWORD, ENCODED_PASSWORD);
        verify(jwtUtil).generateAccessToken(USERNAME, List.of(ROLE_USER));
        verify(refreshTokenService).createRefreshToken(USERNAME);
    }

    @Test
    @DisplayName("Authenticate with unknown user throws UsernameNotFoundException")
    void authenticate_userNotFound_throwsException() {
        String unknownUsername = "unknownuser";
        AuthRequest request = this.setupAuthRequest(unknownUsername, PASSWORD);
        when(userService.getUserByUsername(unknownUsername))
                .thenThrow(new UsernameNotFoundException("User not found"));

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> authService.authenticate(request)
        );
        assertEquals("User not found", exception.getMessage());
        verify(userService).getUserByUsername(unknownUsername);
        this.verifyNoTokenGeneration();
    }

    @Test
    @DisplayName("Authenticate with invalid password throws CannotProceedException")
    void authenticate_invalidPassword_throwsException() {
        String wrongPassword = "WrongPassword";
        AuthRequest request = this.setupAuthRequest(USERNAME, wrongPassword);
        when(userService.getUserByUsername(USERNAME)).thenReturn(defaultUser);
        when(passwordEncoder.matches(wrongPassword, ENCODED_PASSWORD)).thenReturn(false);

        CannotProceedException exception = assertThrows(
                CannotProceedException.class,
                () -> authService.authenticate(request)
        );
        assertEquals("Invalid password", exception.getMessage());
        verify(userService).getUserByUsername(USERNAME);
        verify(passwordEncoder).matches(wrongPassword, ENCODED_PASSWORD);
        this.verifyNoTokenGeneration();
    }

    @Test
    @DisplayName("Authenticate with null request throws IllegalArgumentException")
    void authenticate_nullRequest_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.authenticate(null)
        );
        assertEquals("Authentication request or credentials cannot be null", exception.getMessage());
        this.verifyNoInteractions();
    }

    @Test
    @DisplayName("Authenticate with null username throws IllegalArgumentException")
    void authenticate_nullUsername_throwsException() {
        AuthRequest request = this.setupAuthRequest(null, PASSWORD);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.authenticate(request)
        );
        assertEquals("Authentication request or credentials cannot be null", exception.getMessage());
        this.verifyNoInteractions();
    }

    @Test
    @DisplayName("Authenticate with null password throws IllegalArgumentException")
    void authenticate_nullPassword_throwsException() {
        AuthRequest request = this.setupAuthRequest(USERNAME, null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.authenticate(request)
        );
        assertEquals("Authentication request or credentials cannot be null", exception.getMessage());
        this.verifyNoInteractions();
    }

    @Test
    @DisplayName("Authenticate with short username succeeds")
    void authenticate_shortUsername_succeeds() {
        String shortUsername = "ab";
        AuthRequest request = this.setupAuthRequest(shortUsername, PASSWORD);
        User user = new User(shortUsername, ENCODED_PASSWORD, Role.USER);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRefreshToken(REFRESH_TOKEN);
        setupSuccessfulAuthMocks(request, user, refreshToken);

        AuthResponse response = authService.authenticate(request);

        assertNotNull(response);
        assertEquals(ACCESS_TOKEN, response.getAccessToken());
        assertEquals(REFRESH_TOKEN, response.getRefreshToken());
        verify(userService).getUserByUsername(shortUsername);
        verify(passwordEncoder).matches(PASSWORD, ENCODED_PASSWORD);
        verify(jwtUtil).generateAccessToken(shortUsername, List.of(ROLE_USER));
        verify(refreshTokenService).createRefreshToken(shortUsername);
    }

    @Test
    @DisplayName("Refresh token with valid token returns new access token")
    void refreshToken_successfulRefreshing() {
        String refreshTokenValue = REFRESH_TOKEN;
        setupSuccessfulRefreshMocks(refreshTokenValue, defaultUser);

        AuthResponse response = authService.refreshToken(refreshTokenValue);

        assertNotNull(response);
        assertEquals(ACCESS_TOKEN, response.getAccessToken());
        InOrder inOrder = inOrder(jwtUtil, userService);
        inOrder.verify(jwtUtil).validateToken(refreshTokenValue);
        inOrder.verify(jwtUtil).extractUsername(refreshTokenValue);
        inOrder.verify(userService).getUserByUsername(USERNAME);
        inOrder.verify(jwtUtil).generateAccessToken(USERNAME, List.of(ROLE_USER));
    }

    @Test
    @DisplayName("Refresh token with invalid token throws IllegalArgumentException")
    void refreshToken_invalidRefreshToken_throwsException() {
        String refreshToken = "invalid-refresh-token";
        when(jwtUtil.validateToken(refreshToken))
                .thenThrow(new IllegalArgumentException("Invalid or expired refresh token"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.refreshToken(refreshToken)
        );
        assertEquals("Invalid or expired refresh token", exception.getMessage());
        verify(jwtUtil).validateToken(refreshToken);
        verify(jwtUtil, never()).extractUsername(anyString());
        verify(userService, never()).getUserByUsername(anyString());
        verify(jwtUtil, never()).generateAccessToken(anyString(), anyList());
    }

    @Test
    @DisplayName("Refresh token with valid token but missing user throws ResourceNotFoundException")
    void refreshToken_userNotFound_throwsException() {
        String refreshToken = REFRESH_TOKEN;
        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn(USERNAME);
        when(userService.getUserByUsername(USERNAME))
                .thenThrow(new ResourceNotFoundException("User not found with username: " + USERNAME));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> authService.refreshToken(refreshToken)
        );
        assertEquals("User not found with username: " + USERNAME, exception.getMessage());
        verify(jwtUtil).validateToken(refreshToken);
        verify(jwtUtil).extractUsername(refreshToken);
        verify(userService).getUserByUsername(USERNAME);
        verify(jwtUtil, never()).generateAccessToken(anyString(), anyList());
    }

    @Test
    @DisplayName("Logout with valid refresh token deletes token by username")
    void logout_deletesRefreshTokenByUsername() {
        // Arrange
        String refreshToken = REFRESH_TOKEN;
        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn(USERNAME);
        doNothing().when(refreshTokenService).deleteByUsername(USERNAME);

        // Act
        authService.logout(refreshToken);

        // Assert
        InOrder inOrder = inOrder(jwtUtil, refreshTokenService);
        inOrder.verify(jwtUtil).validateToken(refreshToken);
        inOrder.verify(jwtUtil).extractUsername(refreshToken);
        inOrder.verify(refreshTokenService).deleteByUsername(USERNAME);
    }

    @Test
    @DisplayName("Logout with invalid refresh token throws IllegalArgumentException")
    void logout_invalidToken_throwsException() {
        String refreshToken = "invalid-refresh-token";
        when(jwtUtil.validateToken(refreshToken)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.logout(refreshToken)
        );
        assertEquals("Invalid or expired refresh token", exception.getMessage());
        verify(jwtUtil).validateToken(refreshToken);
        verify(jwtUtil, never()).extractUsername(refreshToken);
        verify(refreshTokenService, never()).deleteByUsername(anyString());
    }

    private void verifyNoTokenGeneration() {
        verify(jwtUtil, never()).generateAccessToken(anyString(), anyList());
        verify(refreshTokenService, never()).createRefreshToken(anyString());
    }

    private void verifyNoInteractions() {
        verify(userService, never()).getUserByUsername(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateAccessToken(anyString(), anyList());
        verify(refreshTokenService, never()).createRefreshToken(anyString());
    }
}