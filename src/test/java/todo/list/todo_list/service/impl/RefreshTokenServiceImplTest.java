package todo.list.todo_list.service.impl;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import todo.list.todo_list.entity.RefreshToken;
import todo.list.todo_list.repository.RefreshTokenRepository;
import todo.list.todo_list.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    private final String username = "testuser";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Test
    @DisplayName("Create refresh token with valid username creates token")
    void createRefreshToken_successfulCreation() {
        when(jwtUtil.generateRefreshToken(username)).thenReturn("jwt.refresh.token");
        when(jwtUtil.getRefreshExpirationMillis()).thenReturn(604800000L); // 7 days
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(username);

        assertNotNull(token);
        assertEquals(username, token.getUsername());
        assertEquals("jwt.refresh.token", token.getRefreshToken());
        assertNotNull(token.getExpiration());
        assertTrue(token.getExpiration().isAfter(Instant.now()));
        verify(refreshTokenRepository).deleteByUsername(username);
        verify(jwtUtil).generateRefreshToken(username);
        verify(jwtUtil).getRefreshExpirationMillis();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Create refresh token with null username throws IllegalArgumentException")
    void createRefreshToken_nullUsername_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenService.createRefreshToken(null)
        );
        assertEquals("Username cannot be null", exception.getMessage());
        verify(refreshTokenRepository, never()).deleteByUsername(anyString());
        verify(jwtUtil, never()).generateRefreshToken(anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Delete refresh token by username deletes tokens")
    void deleteByUsername_successfulDeletion() {
        doNothing().when(refreshTokenRepository).deleteByUsername(username);

        refreshTokenService.deleteByUsername(username);

        verify(refreshTokenRepository).deleteByUsername(username);
        verify(jwtUtil, never()).generateRefreshToken(anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    

    @Test
    @DisplayName("Delete refresh token with null username throws IllegalArgumentException")
    void deleteByUsername_nullUsername_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenService.deleteByUsername(null)
        );
        assertEquals("Username cannot be null", exception.getMessage());
        verify(refreshTokenRepository, never()).deleteByUsername(anyString());
    }

    @Test
    @DisplayName("Create refresh token with database failure throws RuntimeException")
    void createRefreshToken_databaseFailure_throwsException() {
        when(jwtUtil.generateRefreshToken(username)).thenReturn("jwt.refresh.token");
        when(jwtUtil.getRefreshExpirationMillis()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenThrow(new RuntimeException("Database failure"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> refreshTokenService.createRefreshToken(username)
        );
        assertEquals("Database failure", exception.getMessage());
        verify(refreshTokenRepository).deleteByUsername(username);
        verify(jwtUtil).generateRefreshToken(username);
        verify(jwtUtil).getRefreshExpirationMillis();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
}
