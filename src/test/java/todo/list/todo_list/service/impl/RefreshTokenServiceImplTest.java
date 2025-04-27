package todo.list.todo_list.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import todo.list.todo_list.entity.RefreshToken;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.exception.ResourceNotFoundException;
import todo.list.todo_list.model.Role;
import todo.list.todo_list.repository.RefreshTokenRepository;
import todo.list.todo_list.security.JwtUtil;
import todo.list.todo_list.service.UserService;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private RefreshToken setupRefreshToken(String refreshToken, String username, Instant expirationTime) {
        RefreshToken token = new RefreshToken();
        token.setRefreshToken(refreshToken);
        token.setUsername(username);
        token.setExpiration(expirationTime);

        return token;
    }

    private User setupUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);

        return user;
    }

    @Test
    @DisplayName("Generate New Access Token with valid token returns Access Token")
    void generateNewAccessToken_successfulGeneration() {
        String refreshToken = "valid-token";
        String username = "testuser";
        Instant future = Instant.now().plusSeconds(3600);
        RefreshToken storedToken = this.setupRefreshToken(refreshToken, username, future);

        User user = this.setupUser(username, Role.USER);

        when(this.refreshTokenRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(storedToken));
        when(this.userService.getUserByUsername(username)).thenReturn(user);
        when(this.jwtUtil.generateAccessToken(username, List.of("ROLE_USER"))).thenReturn("new-access-token");

        String result = this.refreshTokenService.generateNewAccessToken(refreshToken);
        assertEquals("new-access-token", result);
        verify(this.refreshTokenRepository).findByRefreshToken(refreshToken);
        verify(this.userService).getUserByUsername(username);
        verify(this.jwtUtil).generateAccessToken(username, List.of("ROLE_USER"));
    }

    @Test
    @DisplayName("Generate New Access Token but Refrsh token NOT found returns NULL")
    void generateNewAccessToken_refreshTokenNotFound() {
        String refreshToken = "invalid-token";

        when(this.refreshTokenRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.empty());

        String result = this.refreshTokenService.generateNewAccessToken(refreshToken);

        assertNull(result);
        verify(this.refreshTokenRepository).findByRefreshToken(refreshToken);
        verify(this.userService, never()).getUserByUsername(anyString());
        verify(this.jwtUtil, never()).generateAccessToken(anyString(), anyList());
    }

    @Test
    @DisplayName("Generate New Access Token but Refresh Token EXPIRED returns NULL")
    void generateNewAccessToken_refreshTokenExpired() {
        String refreshToken = "valid-token";
        String username = "testuser";
        Instant past = Instant.now().minusSeconds(3600);
        RefreshToken storedToken = this.setupRefreshToken(refreshToken, username, past);

        when(this.refreshTokenRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(storedToken));
        String result = this.refreshTokenService.generateNewAccessToken(refreshToken);

        assertNull(result);
        verify(this.refreshTokenRepository).findByRefreshToken(refreshToken);
        verify(this.userService, never()).getUserByUsername(anyString());
        verify(this.jwtUtil, never()).generateAccessToken(anyString(), anyList());
    }

    @Test
    @DisplayName("Generate New Access Token but Refresh Token is NULL throws IllegalArgumentException")
    void generateNewAccessToken_nullToken_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.refreshTokenService.generateNewAccessToken(null)
        );
        assertEquals("Refresh token cannot be null", exception.getMessage());
        verify(this.refreshTokenRepository, never()).findByRefreshToken(anyString());
        verify(this.userService, never()).getUserByUsername(anyString());
        verify(this.jwtUtil, never()).generateAccessToken(anyString(), anyList());
    }

    @Test
    @DisplayName("Generate New Access Token but User not found throws Exception")
    void generateNewAccessToken_userNotFound_throwsException() {
        String refreshToken = "valid-token";
        String username = "testuser";
        Instant future = Instant.now().plusSeconds(3600);
        RefreshToken storedToken = this.setupRefreshToken(refreshToken, username, future);

        this.setupUser(username, Role.USER);

        when(this.refreshTokenRepository.findByRefreshToken(refreshToken)).thenReturn(Optional.of(storedToken));
        when(this.userService.getUserByUsername(username)).thenThrow(new ResourceNotFoundException("User not found with username: " + username));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> this.refreshTokenService.generateNewAccessToken(refreshToken)
        );

        assertEquals("User not found with username: " + username, exception.getMessage());
        verify(this.refreshTokenRepository).findByRefreshToken(refreshToken);
        verify(this.userService).getUserByUsername(username);
        verify(this.jwtUtil, never()).generateAccessToken(anyString(), anyList());
    }

    @Test
    @DisplayName("Create Refresh Token with valid username returns new Refresh Token")
    void createRefreshToken_successfulCreation() {
        String username = "testuser";
        String token = "refresh-token";
        long expirationMillis = 86400;
        Instant expectedExpiration = Instant.now().plusMillis(expirationMillis);

        when(this.jwtUtil.generateRefreshToken(username)).thenReturn(token);
        when(this.jwtUtil.getRefreshExpirationMillis()).thenReturn(expirationMillis);
        when(this.refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = this.refreshTokenService.createRefreshToken(username);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(token, result.getRefreshToken());
        assertTrue(result.getExpiration().isAfter(Instant.now()));
        assertTrue(result.getExpiration().isBefore(expectedExpiration.plusSeconds(1)));

        verify(this.jwtUtil).generateRefreshToken(username);
        verify(this.jwtUtil).getRefreshExpirationMillis();
        verify(this.refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Create Refresh Token but username is NULL throws IllegalArgumentException")
    void createRefreshToken_nullUsername_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.refreshTokenService.createRefreshToken(null)
        );

        assertEquals("Username cannot be null", exception.getMessage());
        verify(this.jwtUtil, never()).generateRefreshToken(anyString());
        verify(this.refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete Refresh Token by Username but Username is NULL throws IllegalArgumentException")
    void deleteByUsername_nullUsername_throwsExcepton() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.refreshTokenService.deleteByUsername(null)
        );

        assertEquals("Username cannot be null", exception.getMessage());

        verify(this.refreshTokenRepository, never()).findByUsername(anyString());
    }
}
