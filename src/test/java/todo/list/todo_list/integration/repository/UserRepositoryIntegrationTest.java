package todo.list.todo_list.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDateTime;
import java.util.Optional;

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

    private final String username = "testuser";

    private final String email = "test@example.com";

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
        User user = this.setupUser(this.username, this.email);
        user.setFirstName("Test");
        user.setLastName("User");

        User savedUser = this.userRepository.save(user);

        assertNotNull(savedUser.getId());
        assertEquals(this.username, savedUser.getUsername());
        assertEquals(this.email, savedUser.getEmail());
        assertEquals(Role.USER, savedUser.getRole());
        assertEquals("Test", savedUser.getFirstName());
        assertEquals("User", savedUser.getLastName());
        assertNotNull(savedUser.getCreatedAt());
    }

    @Test
    @DisplayName("Find User by ID when user exists returns User")
    void findById_userExists_returnsUser() {
        User user = this.setupUser(this.username, this.email);
        User savedUser = this.userRepository.save(user);

        Optional<User> result = this.userRepository.findById(savedUser.getId());

        assertTrue(result.isPresent());
        assertEquals(savedUser.getId(), result.get().getId());
        assertEquals(this.username, result.get().getUsername());
    }

    @Test
    @DisplayName("Find User by username when user exists returns User")
    void findByUsername_ShouldReturnUser_WhenUserExists() {
        User user = this.setupUser(this.username, this.email);
        this.userRepository.save(user);

        Optional<User> result = this.userRepository.findByUsername(this.username);

        assertTrue(result.isPresent());
        assertEquals(this.username, result.get().getUsername());
        assertEquals(this.email, result.get().getEmail());
        assertEquals(Role.USER, result.get().getRole());
    }

    @Test
    @DisplayName("Exists by username when username exists returns true")
    void existsByUsername_ShouldReturnTrue_WhenUsernameExists() {
        User user = this.setupUser("uniqueuser", "unique@example.com");
        this.userRepository.save(user);

        boolean exists = this.userRepository.existsByUsername("uniqueuser", null);

        assertTrue(exists);
    }

    @Test
    @DisplayName("Exists by username when username does not exist returns false")
    void existsByUsername_ShouldReturnFalse_WhenUsernameDoesNotExist() {
        boolean exists = this.userRepository.existsByUsername("nonexistent", null);

        assertFalse(exists);
    }

    @Test
    @DisplayName("Exists by email when email exists returns true")
    void existsByEmail_ShouldReturnTrue_WhenEmailExists() {
        User user = this.setupUser("emailuser", "email@example.com");
        this.userRepository.save(user);

        boolean exists = this.userRepository.existsByEmail("email@example.com", null);

        assertTrue(exists);
    }

    @Test
    @DisplayName("Exists by email when email does not exist returns false")
    void existsByEmail_ShouldReturnFalse_WhenEmailDoesNotExist() {
        boolean exists = this.userRepository.existsByEmail("absent@example.com", null);

        assertFalse(exists);
    }

    @Test
    @DisplayName("Save user with null email throws DataIntegrityViolationException")
    void saveUser_WithNullEmail_ShouldThrowException() {
        User user = this.setupUser(this.username, null);

        try {
            this.userRepository.saveAndFlush(user);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Save user with duplicate username throws DataIntegrityViolationException")
    void saveUser_DuplicateUsername_ShouldThrowException() {
        User firstUser = this.setupUser(this.username, "testuser1@example.com");
        this.userRepository.saveAndFlush(firstUser);

        User duplicate = this.setupUser(this.username, "testuser2@example.com");

        try {
            this.userRepository.saveAndFlush(duplicate);
            fail("Expected DataIntegrityViolationException but none was thrown");
        } catch (DataIntegrityViolationException e) {
            assertTrue(true);
        }
    }
}
