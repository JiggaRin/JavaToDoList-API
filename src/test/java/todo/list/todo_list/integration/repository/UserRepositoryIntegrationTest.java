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

    @Autowired
    private UserRepository userRepository;

    private User setupUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    @DisplayName("Save User with valid data persists and returns User")
    void saveUser_validData_successfulSave() {
        User user = setupUser("testuser", "test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals(Role.USER, savedUser.getRole());
        assertEquals("Test", savedUser.getFirstName());
        assertEquals("User", savedUser.getLastName());
        assertNotNull(savedUser.getCreatedAt());
    }

    @Test
    @DisplayName("Find User by ID when user exists returns User")
    void findById_userExists_returnsUser() {
        User user = setupUser("testuser", "test@example.com");
        User savedUser = userRepository.save(user);

        Optional<User> result = userRepository.findById(savedUser.getId());

        assertTrue(result.isPresent());
        assertEquals(savedUser.getId(), result.get().getId());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    @DisplayName("Find User by username when user exists returns User")
    void findByUsername_ShouldReturnUser_WhenUserExists() {
        User user = setupUser("testuser", "test@example.com");
        userRepository.save(user);

        Optional<User> result = userRepository.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());
        assertEquals(Role.USER, result.get().getRole());
    }

    @Test
    @DisplayName("Exists by username when username exists returns true")
    void existsByUsername_ShouldReturnTrue_WhenUsernameExists() {
        User user = setupUser("uniqueuser", "unique@example.com");
        userRepository.save(user);

        boolean exists = userRepository.existsByUsername("uniqueuser", null);

        assertTrue(exists);
    }

    @Test
    @DisplayName("Exists by username when username does not exist returns false")
    void existsByUsername_ShouldReturnFalse_WhenUsernameDoesNotExist() {
        boolean exists = userRepository.existsByUsername("nonexistent", null);

        assertFalse(exists);
    }

    @Test
    @DisplayName("Exists by email when email exists returns true")
    void existsByEmail_ShouldReturnTrue_WhenEmailExists() {
        User user = setupUser("emailuser", "email@example.com");
        userRepository.save(user);

        boolean exists = userRepository.existsByEmail("email@example.com", null);

        assertTrue(exists);
    }

    @Test
    @DisplayName("Exists by email when email does not exist returns false")
    void existsByEmail_ShouldReturnFalse_WhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("absent@example.com", null);

        assertFalse(exists);
    }

    @Test
    @DisplayName("Save user with null email throws DataIntegrityViolationException")
    void saveUser_WithNullEmail_ShouldThrowException() {
        User user = setupUser("testuser", null);

        try {
            userRepository.saveAndFlush(user);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save user with duplicate username throws DataIntegrityViolationException")
    void saveUser_DuplicateUsername_ShouldThrowException() {
        User firstUser = setupUser("testuser", "testuser1@example.com");
        userRepository.saveAndFlush(firstUser);

        User duplicate = setupUser("testuser", "testuser2@example.com");

        try {
            userRepository.saveAndFlush(duplicate);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }
}
