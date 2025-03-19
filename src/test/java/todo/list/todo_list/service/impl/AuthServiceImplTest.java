package todo.list.todo_list.service.impl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import todo.list.todo_list.dto.Auth.AuthRequest;
import todo.list.todo_list.dto.Auth.AuthResponse;
import todo.list.todo_list.entity.RefreshToken;
import todo.list.todo_list.entity.User;
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
}
