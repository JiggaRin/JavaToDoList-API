package todo.list.todo_list.service.impl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import todo.list.todo_list.dto.Auth.AuthRequest;
import todo.list.todo_list.dto.Auth.AuthResponse;
import todo.list.todo_list.entity.RefreshToken;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.exception.ResourceNotFoundException;
import todo.list.todo_list.model.Role;
import todo.list.todo_list.security.JwtUtil;
import todo.list.todo_list.service.RefreshTokenService;
import todo.list.todo_list.service.UserService;

class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void authenticate_successfulAuthentication() {
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("Password123!");

        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRefreshToken("refresh-token-value");

        when(userService.getUserByUsername("testuser")).thenReturn(user);
        when(jwtUtil.generateAccessToken("testuser", List.of("ROLE_USER"))).thenReturn("access-token-value");
        when(refreshTokenService.createRefreshToken("testuser")).thenReturn(refreshToken);

        AuthResponse response = authService.authenticate(request);

        assertNotNull(response);
        assertEquals("access-token-value", response.getAccessToken());
        assertEquals("refresh-token-value", response.getRefreshToken());
        verify(userService).getUserByUsername("testuser");
        verify(jwtUtil).generateAccessToken("testuser", List.of("ROLE_USER"));
        verify(refreshTokenService).createRefreshToken("testuser");
    }

    @Test
    void authenticate_userNotFound_throwsException() {
        AuthRequest request = new AuthRequest();
        request.setUsername("unknownuser");
        request.setPassword("Password123!");

        when(userService.getUserByUsername("unknownuser"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> authService.authenticate(request)
        );
        assertEquals("User not found", exception.getMessage());
        verify(userService).getUserByUsername("unknownuser");
        verify(jwtUtil, never()).generateAccessToken(anyString(), anyList());
        verify(refreshTokenService, never()).createRefreshToken(anyString());
    }

    @Test
    void logout_deletesRefreshTokenByUsername() {
        String refreshToken = "valid-refresh-token";
        String username = "testuser";

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn(username);

        doNothing().when(refreshTokenService).deleteByUsername(username);

        authService.logout(refreshToken);

        InOrder inOrder = inOrder(jwtUtil, refreshTokenService);
        inOrder.verify(jwtUtil).validateToken(refreshToken);
        inOrder.verify(jwtUtil).extractUsername(refreshToken);
        inOrder.verify(refreshTokenService).deleteByUsername(username);

        verify(jwtUtil).extractUsername(refreshToken);
        verify(refreshTokenService).deleteByUsername(username);
    }

    @Test
    void logout_invalidToken_throwsException() {
        String refreshToken = "invalid-refresh-token";

        when(jwtUtil.validateToken(refreshToken)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.logout(refreshToken)
        );
        assertEquals("Invalid refresh token", exception.getMessage());
        verify(jwtUtil).validateToken(refreshToken);
        verify(jwtUtil, never()).extractUsername(refreshToken);
        verify(refreshTokenService, never()).deleteByUsername(anyString());
    }

    @Test
    void refreshToken_successfulRefreshing() {
        String refreshToken = "valid-refresh-token";
        RefreshToken token = new RefreshToken();
        token.setRefreshToken(refreshToken);

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("testuser");

        User user = new User();
        user.setUsername("testuser");
        user.setRole(Role.USER);

        when(userService.getUserByUsername("testuser")).thenReturn(user);
        when(jwtUtil.generateAccessToken("testuser", List.of("ROLE_USER"))).thenReturn("new-access-token");

        AuthResponse response = authService.refreshToken(refreshToken);
        assertNotNull(response);

        InOrder inOrder = inOrder(jwtUtil, userService);
        inOrder.verify(jwtUtil).validateToken(refreshToken);
        inOrder.verify(jwtUtil).extractUsername(refreshToken);
        inOrder.verify(userService).getUserByUsername(user.getUsername());
        inOrder.verify(jwtUtil).generateAccessToken(user.getUsername(), List.of("ROLE_USER"));

        verify(jwtUtil).validateToken(refreshToken);
        verify(jwtUtil).extractUsername(refreshToken);
        verify(userService).getUserByUsername(user.getUsername());
        verify(jwtUtil).generateAccessToken(user.getUsername(), List.of("ROLE_USER"));
    }

    @Test
    void refreshToken_invalidRefreshToken_throwsException() {
        String refreshToken = "invalid-refresh-token";

        when(jwtUtil.validateToken(refreshToken))
                .thenThrow(new IllegalArgumentException("Invalid or expired refresh token"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.refreshToken(refreshToken);
        });

        assertEquals("Invalid or expired refresh token", exception.getMessage());

        verify(jwtUtil).validateToken(refreshToken);
        verify(jwtUtil, never()).extractUsername(anyString());
        verify(userService, never()).getUserByUsername(anyString());
        verify(jwtUtil, never()).generateAccessToken(anyString(), List.of(anyString()));
    }

    @Test
    void refreshToken_userNotFound_throwsException() {
        String refreshToken = "valid-refresh-token";
        String username = "testuser";
        RefreshToken token = new RefreshToken();
        token.setRefreshToken(refreshToken);

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn(username);

        when(userService.getUserByUsername("testuser"))
                .thenThrow(new ResourceNotFoundException("User not found with username: " + username));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            authService.refreshToken(refreshToken);
        });

        assertEquals("User not found with username: " + username, exception.getMessage());

        verify(jwtUtil).validateToken(refreshToken);
        verify(jwtUtil).extractUsername(refreshToken);
        verify(userService).getUserByUsername(username);
        verify(jwtUtil, never()).generateAccessToken(anyString(), List.of(anyString()));
    }
}
