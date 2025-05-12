package todo.list.todo_list.integration.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import todo.list.todo_list.entity.User;
import todo.list.todo_list.model.Role;
import todo.list.todo_list.repository.UserRepository;

@DataJpaTest
@EntityScan("todo.list.todo_list.entity")
@ActiveProfiles("test")
class UserRepositoryIntegrationTest {

    private static final String USERNAME = "testuser";
    private static final String EMAIL = "test@example.com";
    private static final String UNIQUE_USERNAME = "uniqueuser";
    private static final String UNIQUE_EMAIL = "unique@example.com";
    private static final String EMAIL_USER = "emailuser";
    private static final String EMAIL_USER_EMAIL = "email@example.com";
    private static final String NON_EXISTENT_USERNAME = "nonexistent";
    private static final String NON_EXISTENT_EMAIL = "absent@example.com";
    private static final String PASSWORD = "password";
    private static final Role DEFAULT_ROLE = Role.USER;

    @Autowired
    private UserRepository userRepository;

    private User setupUser(String username, String email) {
        User user = new User(username, PASSWORD, DEFAULT_ROLE);
        user.setEmail(email);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    @DisplayName("Save User with valid data persists and returns User")
    void saveUser_validData_successfulSave() {
        // Arrange
        User user = setupUser(USERNAME, EMAIL);
        user.setFirstName("Test");
        user.setLastName("User");

        // Act
        User savedUser = userRepository.save(user);

        // Assert
        assertNotNull(savedUser.getId(), "User ID should be generated");
        assertEquals(USERNAME, savedUser.getUsername(), "Username should match");
        assertEquals(EMAIL, savedUser.getEmail(), "Email should match");
        assertEquals(DEFAULT_ROLE, savedUser.getRole(), "Role should match");
        assertEquals("Test", savedUser.getFirstName(), "First name should match");
        assertEquals("User", savedUser.getLastName(), "Last name should match");
        assertNotNull(savedUser.getCreatedAt(), "Created timestamp should be set");
    }

    @Test
    @DisplayName("Find User by ID when user exists returns User")
    void findById_userExists_returnsUser() {
        // Arrange
        User user = setupUser(USERNAME, EMAIL);
        User savedUser = userRepository.save(user);

        // Act
        Optional<User> result = userRepository.findById(savedUser.getId());

        // Assert
        assertTrue(result.isPresent(), "User should be found");
        assertEquals(savedUser.getId(), result.get().getId(), "User ID should match");
        assertEquals(USERNAME, result.get().getUsername(), "Username should match");
    }

    @Test
    @DisplayName("Find User by username when user exists returns User")
    void findByUsername_userExists_returnsUser() {
        // Arrange
        User user = setupUser(USERNAME, EMAIL);
        userRepository.save(user);

        // Act
        Optional<User> result = userRepository.findByUsername(USERNAME);

        // Assert
        assertTrue(result.isPresent(), "User should be found");
        assertEquals(USERNAME, result.get().getUsername(), "Username should match");
        assertEquals(EMAIL, result.get().getEmail(), "Email should match");
        assertEquals(DEFAULT_ROLE, result.get().getRole(), "Role should match");
    }

    @Test
    @DisplayName("Exists by username when username exists returns true")
    void existsByUsername_usernameExists_returnsTrue() {
        // Arrange
        User user = setupUser(UNIQUE_USERNAME, UNIQUE_EMAIL);
        userRepository.save(user);

        // Act
        boolean exists = userRepository.existsByUsername(UNIQUE_USERNAME, null);

        // Assert
        assertTrue(exists, "Username should exist");
    }

    @Test
    @DisplayName("Exists by username when username does not exist returns false")
    void existsByUsername_usernameDoesNotExist_returnsFalse() {
        // Arrange
        // No setup needed, testing non-existent username

        // Act & Assert
        boolean exists = userRepository.existsByUsername(NON_EXISTENT_USERNAME, null);
        assertFalse(exists, "Non-existent username should not exist");
    }

    @Test
    @DisplayName("Exists by email when email exists returns true")
    void existsByEmail_emailExists_returnsTrue() {
        // Arrange
        User user = setupUser(EMAIL_USER, EMAIL_USER_EMAIL);
        userRepository.save(user);

        // Act
        boolean exists = userRepository.existsByEmail(EMAIL_USER_EMAIL, null);

        // Assert
        assertTrue(exists, "Email should exist");
    }

    @Test
    @DisplayName("Exists by email when email does not exist returns false")
    void existsByEmail_emailDoesNotExist_returnsFalse() {
        // Arrange
        // No setup needed, testing non-existent email

        // Act & Assert
        boolean exists = userRepository.existsByEmail(NON_EXISTENT_EMAIL, null);
        assertFalse(exists, "Non-existent email should not exist");
    }

    @Test
    @DisplayName("Save User with null email throws DataIntegrityViolationException")
    void saveUser_nullEmail_throwsException() {
        // Arrange
        User user = setupUser(USERNAME, null);

        // Act & Assert
        try {
            userRepository.saveAndFlush(user);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save User with duplicate username throws DataIntegrityViolationException")
    void saveUser_duplicateUsername_throwsException() {
        // Arrange
        User firstUser = setupUser(USERNAME, EMAIL);
        userRepository.saveAndFlush(firstUser);
        User duplicateUser = setupUser(USERNAME, "testuser2@example.com");

        // Act & Assert
        try {
            userRepository.saveAndFlush(duplicateUser);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }
}
