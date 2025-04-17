package todo.list.todo_list.service.impl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import todo.list.todo_list.dto.Registration.RegistrationRequest;
import todo.list.todo_list.dto.Registration.RegistrationResponse;
import todo.list.todo_list.dto.User.ChangePasswordRequest;
import todo.list.todo_list.dto.User.UpdateRequest;
import todo.list.todo_list.dto.User.UserDTO;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.exception.CannotProceedException;
import todo.list.todo_list.exception.ResourceNotFoundException;
import todo.list.todo_list.exception.UserAlreadyExistsException;
import todo.list.todo_list.mapper.UserMapper;
import todo.list.todo_list.model.Role;
import todo.list.todo_list.repository.RefreshTokenRepository;
import todo.list.todo_list.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Register User with valid data returns RegistrationResponse")
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
        user.setEmail("test@example.com");
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
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());

        verify(userRepository).existsByUsername("testuser", null);
        verify(userRepository).existsByEmail("test@example.com", null);
        verify(userMapper).fromRegistrationRequest(request);
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Register User with Username which is already existed throws UserAlreadyExistsException")
    void registerUser_duplicateUsername_throwsException() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");

        when(userRepository.existsByUsername("testuser", null)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.registerUser(request)
        );
        assertEquals("Username is already taken!", exception.getMessage());
        verify(userRepository).existsByUsername("testuser", null);
        verify(userRepository, never()).existsByEmail(anyString(), any());
        verify(userMapper, never()).fromRegistrationRequest(any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register User with email which is already in use by another user throws UserAlreadyExistsException")
    void registerUser_duplicateEmail_throwsException() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Password123!");

        when(userRepository.existsByUsername("testuser", null)).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com", null)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.registerUser(request)
        );
        assertEquals("Email is already in use!", exception.getMessage());

        verify(userRepository).existsByUsername("testuser", null);
        verify(userRepository).existsByEmail("test@example.com", null);
        verify(userMapper, never()).fromRegistrationRequest(any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register User but Registration request in NULL throws IllegalArgumentException")
    void registerUser_nullRequest_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(null)
        );
        assertEquals("Registration request cannot be null", exception.getMessage());

        verify(userRepository, never()).existsByUsername(anyString(), anyLong());
        verify(userRepository, never()).existsByEmail(anyString(), anyLong());
        verify(userMapper, never()).fromRegistrationRequest(any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update User with valid data returns UserDTO")
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
        userDTO.setFirstName("NewFirst");
        userDTO.setLastName("NewLast");
        userDTO.setEmail("new@example.com");

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
    @DisplayName("Update User with User ID which is not found throws ResourceNotFoundException")
    void updateUser_userNotFound_throwsException() {
        Long userId = 1L;
        UpdateRequest request = new UpdateRequest();
        request.setEmail("new@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.updateUser(userId, request)
        );
        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository, never()).existsByEmail(anyString(), anyLong());
        verify(userMapper, never()).updateUserFromRequest(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update User with email which is already in use by another user throws UserAlreadyExistsException")
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

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.updateUser(userId, request)
        );

        assertEquals("Email is already in use!", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmail("new@example.com", userId);
        verify(userMapper, never()).updateUserFromRequest(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update User but userId is NULL throws updateUser_nullUserId_throwsIllegalArgumentException")
    void updateUser_nullUserId_throwsException() {
        UpdateRequest request = new UpdateRequest();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(null, request)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Update User but Update request in NULL throws IllegalArgumentException")
    void updateUser_nullRequest_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(1L, null)
        );
        assertEquals("Update request cannot be null", exception.getMessage());

        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).existsByEmail(anyString(), anyLong());
        verify(userMapper, never()).updateUserFromRequest(any(), any());
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toUserDTO(any());
    }

    @Test
    @DisplayName("Change Password with valid data returns void")
    void changePassword_successfulChange() {
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("Password123!");
        request.setNewPassword("Password123!!");

        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setPassword("oldHashedPass");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "oldHashedPass")).thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("encodedPass");
        when(userRepository.save(user)).thenReturn(user);

        userService.changePassword(userId, request);

        InOrder inOrder = inOrder(userRepository, refreshTokenRepository, passwordEncoder);
        inOrder.verify(userRepository).findById(userId);
        inOrder.verify(passwordEncoder).matches("Password123!", "oldHashedPass");
        inOrder.verify(passwordEncoder).encode("Password123!!");
        inOrder.verify(userRepository).save(user);
        inOrder.verify(refreshTokenRepository).deleteByUsername("testuser");
    }

    @Test
    @DisplayName("Change Password with incorect Old Password throws CannotProceedException")
    void changePassword_wrongOldPassword_throwsException() {
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("Password123!");
        request.setNewPassword("Password123!!");

        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setPassword("oldHashedPass");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getOldPassword(), user.getPassword())).thenReturn(false);

        CannotProceedException exception = assertThrows(
                CannotProceedException.class,
                () -> userService.changePassword(userId, request)
        );
        assertEquals("Old password is incorrect", exception.getMessage());

        verify(userRepository).findById(userId);
        verify(passwordEncoder).matches(request.getOldPassword(), user.getPassword());
        verify(passwordEncoder, never()).encode("Password123!!");
        verify(userRepository, never()).save(user);
        verify(refreshTokenRepository, never()).deleteByUsername("testuser");
    }

    @Test
    @DisplayName("Change Password but userId is NULL throws IllegalArgumentException")
    void changePassword_nullUserId_throwsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.changePassword(null, request)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Change Password but Change Password request in NULL throws IllegalArgumentException")
    void changePassword_nullRequest_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.changePassword(1L, null)
        );
        assertEquals("Change Password request cannot be null", exception.getMessage());

        verify(userRepository, never()).findById(anyLong());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).deleteByUsername(anyString());
    }

    @Test
    @DisplayName("Get User by ID but userId is NULL throws IllegalArgumentException")
    void getUserById_nullUserId_throwsEsception() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(null)
        );

        assertEquals("User ID cannot be null", exception.getMessage());

        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Get User by Username but Username is NULL throws IllegalArgumentException")
    void getUserByUsername_nullUsername_throwsExcepton() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserByUsername(null)
        );

        assertEquals("Username cannot be null", exception.getMessage());

        verify(userRepository, never()).findByUsername(anyString());
    }
}
