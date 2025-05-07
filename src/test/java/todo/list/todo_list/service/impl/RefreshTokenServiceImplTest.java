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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import todo.list.todo_list.entity.RefreshToken;
import todo.list.todo_list.repository.RefreshTokenRepository;
import todo.list.todo_list.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Test
    @DisplayName("Create Refresh Token with valid username returns new Refresh Token")
    void createRefreshToken_successfulCreation() {
        String username = "testuser";
        String token = "refresh-token";
        long expirationMillis = 86400;
        Instant expectedExpiration = Instant.now().plusMillis(expirationMillis);

        when(jwtUtil.generateRefreshToken(username)).thenReturn(token);
        when(jwtUtil.getRefreshExpirationMillis()).thenReturn(expirationMillis);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(username);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(token, result.getRefreshToken());
        assertTrue(result.getExpiration().isAfter(Instant.now()));
        assertTrue(result.getExpiration().isBefore(expectedExpiration.plusSeconds(1)));

        verify(jwtUtil).generateRefreshToken(username);
        verify(jwtUtil).getRefreshExpirationMillis();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Create Refresh Token but username is NULL throws IllegalArgumentException")
    void createRefreshToken_nullUsername_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenService.createRefreshToken(null)
        );

        assertEquals("Username cannot be null", exception.getMessage());
        verify(jwtUtil, never()).generateRefreshToken(anyString());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete Refresh Token by Username but Username is NULL throws IllegalArgumentException")
    void deleteByUsername_nullUsername_throwsExcepton() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenService.deleteByUsername(null)
        );

        assertEquals("Username cannot be null", exception.getMessage());

        verify(refreshTokenRepository, never()).findByUsername(anyString());
    }
}
