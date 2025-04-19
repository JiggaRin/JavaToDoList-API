package todo.list.todo_list.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

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

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    private RefreshToken setupRefreshToken(String refreshToken, String username, Instant expiration) {
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setExpiration(expiration);
        newRefreshToken.setRefreshToken(refreshToken);
        newRefreshToken.setUsername(username);
        return newRefreshToken;
    }

    @Test
    @DisplayName("Save Refresh Token with valid data persists and returns Refresh Token")
    void saveRefreshToken_validData_successfulSave() {
        Instant expiration = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusSeconds(604800);
        RefreshToken refreshToken = setupRefreshToken("valid-token", "testuser", expiration);

        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        assertNotNull(savedRefreshToken.getId());
        assertEquals("valid-token", savedRefreshToken.getRefreshToken());
        assertEquals("testuser", savedRefreshToken.getUsername());
        assertEquals(expiration, savedRefreshToken.getExpiration());
    }

    @Test
    @DisplayName("Find by Refresh Token with valid token persists and returns Refresh Token")
    void findByRefreshToken_validToken_returnsToken() {
        Instant expiration = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusSeconds(604800);
        RefreshToken refreshToken = setupRefreshToken("valid-token", "testuser", expiration);

        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        Optional<RefreshToken> result = refreshTokenRepository.findByRefreshToken(savedRefreshToken.getRefreshToken());

        assertTrue(result.isPresent());
        assertEquals("valid-token", result.get().getRefreshToken());
        assertEquals("testuser", result.get().getUsername());
        assertEquals(expiration, result.get().getExpiration());
    }

    @Test
    @DisplayName("Find by Refresh Token with token which is not existed")
    void findByRefreshToken_notExists_returnsEmpty() {
        Optional<RefreshToken> result = refreshTokenRepository.findByRefreshToken("non-existed-token");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Find by Username with valid data returns token")
    void findByUsername_validData_returnsToken() {
        Instant expiration = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusSeconds(604800);
        RefreshToken refreshToken = setupRefreshToken("valid-token", "testuser", expiration);

        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        Optional<RefreshToken> result = refreshTokenRepository.findByUsername(savedRefreshToken.getUsername());

        assertTrue(result.isPresent());
        assertEquals("valid-token", result.get().getRefreshToken());
        assertEquals("testuser", result.get().getUsername());
        assertEquals(expiration, result.get().getExpiration());
    }

    @Test
    @DisplayName("Find by Username with username which is not existed")
    void findByUsername_notExists_returnsEmpty() {
        Optional<RefreshToken> result = refreshTokenRepository.findByUsername("non-existed-username");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Delete by Username with valid data deletes token")
    void deleteByUsername_validData_successfulDelete() {
        Instant expiration = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusSeconds(604800);
        RefreshToken refreshToken = setupRefreshToken("valid-token", "testuser", expiration);
        refreshTokenRepository.save(refreshToken);

        refreshTokenRepository.deleteByUsername("testuser");
        Optional<RefreshToken> result = refreshTokenRepository.findByUsername("testuser");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Save Refresh Token with NULL Refresh Token cause DataIntegrityViolationException")
    void saveRefreshToken_nullToken_ShouldThrowException() {
        Instant expiration = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusSeconds(604800);
        RefreshToken refreshToken = setupRefreshToken(null, "testuser", expiration);

        try {
            refreshTokenRepository.saveAndFlush(refreshToken);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Refresh Token with NULL Username cause DataIntegrityViolationException")
    void saveRefreshToken_nullUsername_ShouldThrowException() {
        Instant expiration = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusSeconds(604800);
        RefreshToken refreshToken = setupRefreshToken("valid-token", null, expiration);

        try {
            refreshTokenRepository.saveAndFlush(refreshToken);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Refresh Token with NULL Expiration value cause DataIntegrityViolationException")
    void saveRefreshToken_nullExpiration_ShouldThrowException() {
        RefreshToken refreshToken = setupRefreshToken("valid-token", "testuser", null);

        try {
            refreshTokenRepository.saveAndFlush(refreshToken);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Refresh Token with Duplicate Token cause DataIntegrityViolationException")
    void saveRefreshToken_duplicateToken_ShouldThrowException() {
        Instant expiration = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusSeconds(604800);
        RefreshToken refreshToken = setupRefreshToken("valid-token", "testuser", expiration);
        RefreshToken duplicateRefreshToken = setupRefreshToken("valid-token", "testuser2", expiration);

        refreshTokenRepository.save(refreshToken);

        try {
            refreshTokenRepository.saveAndFlush(duplicateRefreshToken);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save Refresh Token with duplicate Username throws DataIntegrityViolationException")
    void saveRefreshToken_duplicateUsername_ShouldThrowException() {
        Instant expiration = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusSeconds(604800);
        RefreshToken refreshToken = setupRefreshToken("token1", "testuser", expiration);
        RefreshToken duplicateUsernameToken = setupRefreshToken("token2", "testuser", expiration);

        refreshTokenRepository.save(refreshToken);

        try {
            refreshTokenRepository.saveAndFlush(duplicateUsernameToken);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }
}
