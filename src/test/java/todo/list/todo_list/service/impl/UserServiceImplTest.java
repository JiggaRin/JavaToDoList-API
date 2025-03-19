package todo.list.todo_list.service.impl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import todo.list.todo_list.dto.Registration.RegistrationRequest;
import todo.list.todo_list.dto.Registration.RegistrationResponse;
import todo.list.todo_list.dto.User.UpdateRequest;
import todo.list.todo_list.dto.User.UserDTO;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.exception.ResourceNotFoundException;
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
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole(Role.USER);

        User user = new User();
        user.setUsername("testuser");
        user.setPassword("Password123!");
        user.setFirstName("Test");
        user.setRole(Role.USER);

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
        verify(userRepository).existsByUsername("testuser", null);
        verify(userRepository).existsByEmail("test@example.com", null);
        verify(userMapper).fromRegistrationRequest(request);
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(user);
    }

    @Test
    void registerUser_duplicateUsername_throwsException() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");

        when(userRepository.existsByUsername("testuser", null)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(request);
        });
        assertEquals("Username is already taken!", exception.getMessage());
        verify(userRepository).existsByUsername("testuser", null);
        verify(userRepository, never()).existsByEmail(anyString(), any());
        verify(userMapper, never()).fromRegistrationRequest(any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_duplicateEmail_throwsException() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");

        when(userRepository.existsByUsername("testuser", null)).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com", null)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(request);
        });
        assertEquals("Email is already in use!", exception.getMessage());
        verify(userRepository).existsByUsername("testuser", null);
        verify(userRepository).existsByEmail("test@example.com", null);
        verify(userMapper, never()).fromRegistrationRequest(any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_successfulUpdate() {
        Long userId = 1L;
        UpdateRequest request = new UpdateRequest();
        request.setEmail("new@example.com");
        request.setFirstName("NewFirst");
        request.setLastName("NewLast");

        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("old@example.com");
        user.setRole(Role.USER);

        doAnswer(invocation -> {
            User target = invocation.getArgument(1);
            target.setEmail(request.getEmail());
            target.setFirstName(request.getFirstName());
            target.setLastName(request.getLastName());
            return null;
        }).when(userMapper).updateUserFromRequest(request, user);

        UserDTO userDTO = new UserDTO();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com", userId)).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserDTO(user)).thenReturn(userDTO);

        UserDTO response = userService.updateUser(userId, request);

        assertNotNull(response);
        assertSame(userDTO, response);
        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmail("new@example.com", userId);
        verify(userMapper).updateUserFromRequest(request, user);
        verify(userRepository).save(user);
        verify(userMapper).toUserDTO(user);
    }

    @Test
    void updateUser_userNotFound_throwsException() {
        Long userId = 1L;
        UpdateRequest request = new UpdateRequest();
        request.setEmail("new@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUser(userId, request);
        });
        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository, never()).existsByEmail(anyString(), anyLong());
        verify(userMapper, never()).updateUserFromRequest(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_duplicateEmail_throwsException() {
        Long userId = 1L;
        UpdateRequest request = new UpdateRequest();
        request.setEmail("new@example.com");

        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("old@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com", userId)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
            userService.updateUser(userId, request);
        });
        assertEquals("Email is already in use!", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmail("new@example.com", userId);
        verify(userMapper, never()).updateUserFromRequest(any(), any());
        verify(userRepository, never()).save(any());
    }
}