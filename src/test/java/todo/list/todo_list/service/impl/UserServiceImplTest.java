package todo.list.todo_list.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import todo.list.todo_list.dto.Registration.RegistrationRequest;
import todo.list.todo_list.dto.Registration.RegistrationResponse;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.exception.UserAlreadyExistsException;
import todo.list.todo_list.mapper.UserMapper;
import todo.list.todo_list.model.Role;
import todo.list.todo_list.repository.UserRepository;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_successfulRegistration() {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole(Role.USER);

        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("Password123!");

        when(userRepository.existsByUsername("testuser", null)).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com", null)).thenReturn(false);
        when(userMapper.fromRegistrationRequest(request)).thenReturn(user);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setPassword("encodedPassword");
            return savedUser;
        });

        RegistrationResponse response = userService.registerUser(request);

        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        verify(userMapper).fromRegistrationRequest(request);
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(user);
        verify(userRepository).existsByUsername("testuser", null);
        verify(userRepository).existsByEmail("test@example.com", null);
    }

    @Test
    void registerUser_duplicateUsername_throwsException() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole(Role.USER);

        when(userRepository.existsByUsername("testuser", null)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(request);
        });
        assertEquals("Username is already taken!", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).fromRegistrationRequest(any());
        verify(userRepository).existsByUsername("testuser", null);
        verify(userRepository, never()).existsByEmail(anyString(), any());
    }

    @Test
    void registerUser_duplicateEmail_throwsException() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole(Role.USER);

        when(userRepository.existsByUsername("testuser", null)).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com", null)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(request);
        });
        assertEquals("Email is already in use!", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).fromRegistrationRequest(any());
        verify(userRepository).existsByUsername("testuser", null);
        verify(userRepository).existsByEmail("test@example.com", null);
    }
}