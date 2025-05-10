package todo.list.todo_list.service.impl;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
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

    private static final String USERNAME = "testuser";
    private static final String REFRESH_TOKEN = "jwt.refresh.token";
    private static final long REFRESH_EXPIRATION_MILLIS = 604800000L; // 7 days

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private RefreshToken defaultToken;

    @BeforeEach
    @SuppressWarnings("unused")
    void setup() {
        defaultToken = new RefreshToken();
        defaultToken.setUsername(USERNAME);
        defaultToken.setRefreshToken(REFRESH_TOKEN);
        defaultToken.setExpiration(Instant.now().plusMillis(REFRESH_EXPIRATION_MILLIS));
    }

    private void setupSuccessfulRefreshTokenMocks(String username, String refreshToken) {
        when(jwtUtil.generateRefreshToken(username)).thenReturn(refreshToken);
        when(jwtUtil.getRefreshExpirationMillis()).thenReturn(REFRESH_EXPIRATION_MILLIS);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(defaultToken);
    }

    private void setupDatabaseFailureMocks(String username, String refreshToken) {
        when(jwtUtil.generateRefreshToken(username)).thenReturn(refreshToken);
        when(jwtUtil.getRefreshExpirationMillis()).thenReturn(REFRESH_EXPIRATION_MILLIS);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenThrow(new RuntimeException("Database failure"));
    }

    @Test
    @DisplayName("Create refresh token with valid username creates token")
    void createRefreshToken_successfulCreation() {
        // Arrange
        this.setupSuccessfulRefreshTokenMocks(USERNAME, REFRESH_TOKEN);

        // Act
        RefreshToken token = refreshTokenService.createRefreshToken(USERNAME);

        // Assert
        assertNotNull(token);
        assertEquals(USERNAME, token.getUsername());
        assertEquals(REFRESH_TOKEN, token.getRefreshToken());
        assertNotNull(token.getExpiration());
        assertTrue(token.getExpiration().isAfter(Instant.now()));
        verify(refreshTokenRepository).deleteByUsername(USERNAME);
        verify(jwtUtil).generateRefreshToken(USERNAME);
        verify(jwtUtil).getRefreshExpirationMillis();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Create refresh token with null username throws IllegalArgumentException")
    void createRefreshToken_nullUsername_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenService.createRefreshToken(null)
        );
        assertEquals("Username cannot be null", exception.getMessage());
        verify(refreshTokenRepository, never()).deleteByUsername(anyString());
        this.verifyNoRefreshTokenGenerating();
    }

    @Test
    @DisplayName("Create refresh token with database failure throws RuntimeException")
    void createRefreshToken_databaseFailure_throwsException() {
        // Arrange
        this.setupDatabaseFailureMocks(USERNAME, REFRESH_TOKEN);

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> refreshTokenService.createRefreshToken(USERNAME)
        );
        assertEquals("Database failure", exception.getMessage());
        verify(refreshTokenRepository).deleteByUsername(USERNAME);
        verify(jwtUtil).generateRefreshToken(USERNAME);
        verify(jwtUtil).getRefreshExpirationMillis();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Delete refresh token by username succeeds")
    void deleteByUsername_successfulDeletion() {
        // Arrange
        doNothing().when(refreshTokenRepository).deleteByUsername(USERNAME);

        // Act
        refreshTokenService.deleteByUsername(USERNAME);

        // Assert
        verify(refreshTokenRepository).deleteByUsername(USERNAME);
        this.verifyNoRefreshTokenGenerating();
    }

    @Test
    @DisplayName("Delete refresh token with null username throws IllegalArgumentException")
    void deleteByUsername_nullUsername_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenService.deleteByUsername(null)
        );
        assertEquals("Username cannot be null", exception.getMessage());
        verify(refreshTokenRepository, never()).deleteByUsername(anyString());
        this.verifyNoRefreshTokenGenerating();
    }

    private void verifyNoRefreshTokenGenerating() {
        verify(jwtUtil, never()).generateRefreshToken(anyString());
        verify(jwtUtil, never()).getRefreshExpirationMillis();
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}