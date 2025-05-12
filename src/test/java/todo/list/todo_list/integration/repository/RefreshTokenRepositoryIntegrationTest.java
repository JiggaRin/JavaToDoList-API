package todo.list.todo_list.integration.repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import todo.list.todo_list.entity.RefreshToken;
import todo.list.todo_list.repository.RefreshTokenRepository;

@DataJpaTest
@EntityScan("todo.list.todo_list.entity")
@ActiveProfiles("test")
class RefreshTokenRepositoryIntegrationTest {

    private static final String USERNAME = "testuser";
    private static final String USERNAME_2 = "testuser2";
    private static final String REFRESH_TOKEN = "valid-token";
    private static final String REFRESH_TOKEN_2 = "token2";
    private static final String NON_EXISTENT_TOKEN = "non-existed-token";
    private static final String NON_EXISTENT_USERNAME = "non-existed-username";
    private static final long EXPIRATION_SECONDS = 604800; // 7 days
    private static final Instant defaultExpiration = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusSeconds(EXPIRATION_SECONDS);
    ;


    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshToken setupRefreshToken(String refreshToken, String username, Instant expiration) {
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setExpiration(expiration);
        newRefreshToken.setRefreshToken(refreshToken);
        newRefreshToken.setUsername(username);

        return newRefreshToken;
    }

    @Test
    @DisplayName("Save Refresh Token with valid data persists and returns token")
    void saveRefreshToken_validData_successfulSave() {
        // Arrange
        RefreshToken refreshToken = setupRefreshToken(REFRESH_TOKEN, USERNAME, defaultExpiration);

        // Act
        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        // Assert
        assertNotNull(savedRefreshToken.getId(), "Token ID should be generated");
        assertEquals(REFRESH_TOKEN, savedRefreshToken.getRefreshToken(), "Token value should match");
        assertEquals(USERNAME, savedRefreshToken.getUsername(), "Username should match");
        assertEquals(defaultExpiration, savedRefreshToken.getExpiration(), "Expiration should match");
    }

    @Test
    @DisplayName("Find Refresh Token by valid token returns token")
    void findByRefreshToken_validToken_returnsToken() {
        // Arrange
        RefreshToken refreshToken = setupRefreshToken(REFRESH_TOKEN, USERNAME, defaultExpiration);
        refreshTokenRepository.save(refreshToken);

        // Act
        Optional<RefreshToken> result = refreshTokenRepository.findByRefreshToken(REFRESH_TOKEN);

        // Assert
        assertTrue(result.isPresent(), "Token should be found");
        assertEquals(REFRESH_TOKEN, result.get().getRefreshToken(), "Token value should match");
        assertEquals(USERNAME, result.get().getUsername(), "Username should match");
        assertEquals(defaultExpiration, result.get().getExpiration(), "Expiration should match");
    }

    @Test
    @DisplayName("Find Refresh Token by non-existent token returns empty")
    void findByRefreshToken_nonExistentToken_returnsEmpty() {
        // Act & Assert
        Optional<RefreshToken> result = refreshTokenRepository.findByRefreshToken(NON_EXISTENT_TOKEN);
        assertTrue(result.isEmpty(), "Non-existent token should return empty");
    }

    @Test
    @DisplayName("Find Refresh Token by valid username returns token")
    void findByUsername_validUsername_returnsToken() {
        // Arrange
        RefreshToken refreshToken = setupRefreshToken(REFRESH_TOKEN, USERNAME, defaultExpiration);
        refreshTokenRepository.save(refreshToken);

        // Act
        Optional<RefreshToken> result = refreshTokenRepository.findByUsername(USERNAME);

        // Assert
        assertTrue(result.isPresent(), "Token should be found");
        assertEquals(REFRESH_TOKEN, result.get().getRefreshToken(), "Token value should match");
        assertEquals(USERNAME, result.get().getUsername(), "Username should match");
        assertEquals(defaultExpiration, result.get().getExpiration(), "Expiration should match");
    }

    @Test
    @DisplayName("Find Refresh Token by non-existent username returns empty")
    void findByUsername_nonExistentUsername_returnsEmpty() {
        // Act & Assert
        Optional<RefreshToken> result = refreshTokenRepository.findByUsername(NON_EXISTENT_USERNAME);
        assertTrue(result.isEmpty(), "Non-existent username should return empty");
    }

    @Test
    @DisplayName("Delete Refresh Token by valid username deletes token")
    void deleteByUsername_validUsername_successfulDelete() {
        // Arrange
        RefreshToken refreshToken = setupRefreshToken(REFRESH_TOKEN, USERNAME, defaultExpiration);
        refreshTokenRepository.save(refreshToken);

        // Act
        refreshTokenRepository.deleteByUsername(USERNAME);

        // Assert
        Optional<RefreshToken> result = refreshTokenRepository.findByUsername(USERNAME);
        assertTrue(result.isEmpty(), "Token should be deleted");
    }

    @Test
    @DisplayName("Save Refresh Token with null token throws DataIntegrityViolationException")
    void saveRefreshToken_nullToken_ShouldThrowException() {
        // Arrange
        RefreshToken refreshToken = setupRefreshToken(null, USERNAME, defaultExpiration);

        // Act & Assert
        try {
            refreshTokenRepository.saveAndFlush(refreshToken);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Refresh Token with null username throws DataIntegrityViolationException")
    void saveRefreshToken_nullUsername_throwsException() {
        // Arrange
        RefreshToken refreshToken = setupRefreshToken(REFRESH_TOKEN, null, defaultExpiration);

        // Act & Assert
        try {
            refreshTokenRepository.saveAndFlush(refreshToken);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Refresh Token with null expiration throws DataIntegrityViolationException")
    void saveRefreshToken_nullExpiration_throwsException() {
        // Arrange
        RefreshToken refreshToken = setupRefreshToken(REFRESH_TOKEN, USERNAME, null);

        // Act & Assert
        try {
            refreshTokenRepository.saveAndFlush(refreshToken);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Refresh Token with duplicate token throws DataIntegrityViolationException")
    void saveRefreshToken_duplicateToken_throwsException() {
        // Arrange
        RefreshToken refreshToken = setupRefreshToken(REFRESH_TOKEN, USERNAME, defaultExpiration);
        RefreshToken duplicateRefreshToken = setupRefreshToken(REFRESH_TOKEN, USERNAME_2, defaultExpiration);
        refreshTokenRepository.save(refreshToken);

        // Act & Assert
        try {
            refreshTokenRepository.saveAndFlush(duplicateRefreshToken);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Refresh Token with duplicate username throws DataIntegrityViolationException")
    void saveRefreshToken_duplicateUsername_throwsException() {
        // Arrange
        RefreshToken refreshToken = setupRefreshToken(REFRESH_TOKEN, USERNAME, defaultExpiration);
        RefreshToken duplicateUsernameToken = setupRefreshToken(REFRESH_TOKEN_2, USERNAME, defaultExpiration);
        refreshTokenRepository.save(refreshToken);

        // Act & Assert
        try {
            refreshTokenRepository.saveAndFlush(duplicateUsernameToken);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }
}
