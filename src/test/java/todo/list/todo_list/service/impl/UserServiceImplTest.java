package todo.list.todo_list.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    private final String username = "testuser";

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

    private RegistrationRequest setupRegistationRequest(String username, String email, String password) {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);

        return request;
    }

    private User setupUser(String username, String email, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);

        return user;
    }

    private UpdateRequest setupUpdateRequest(String newEmail) {
        UpdateRequest request = new UpdateRequest();
        request.setEmail(newEmail);

        return request;
    }

    private ChangePasswordRequest setupChangePasswordRequest(String oldPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(oldPassword);
        request.setNewPassword(newPassword);

        return request;
    }

    @Test
    @DisplayName("Register User with valid data returns RegistrationResponse")
    void registerUser_successfulRegistration() {
        String email = "test@example.com";
        String password = "Password123!";
        String encodedPassword = "encodedPassword";
        RegistrationRequest request = this.setupRegistationRequest(this.username, email, password);
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole(Role.USER);

        User user = this.setupUser(this.username, email, Role.USER);
        user.setFirstName("Test");

        when(this.userRepository.existsByUsername(this.username, null)).thenReturn(false);
        when(this.userRepository.existsByEmail(email, null)).thenReturn(false);
        when(this.userMapper.fromRegistrationRequest(request)).thenReturn(user);
        when(this.passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(this.userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setPassword(encodedPassword);
            return savedUser;
        });

        RegistrationResponse response = this.userService.registerUser(request);

        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        assertEquals(this.username, response.getUsername());
        assertEquals(email, response.getEmail());

        verify(this.userRepository).existsByUsername(this.username, null);
        verify(this.userRepository).existsByEmail(email, null);
        verify(this.userMapper).fromRegistrationRequest(request);
        verify(this.passwordEncoder).encode(password);
        verify(this.userRepository).save(user);
    }

    @Test
    @DisplayName("Register User with this.username which is already existed throws UserAlreadyExistsException")
    void registerUser_duplicateUsername_throwsException() {
        String email = "test@example.com";
        String password = "Password123!";
        RegistrationRequest request = this.setupRegistationRequest(this.username, email, password);

        when(this.userRepository.existsByUsername(this.username, null)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> this.userService.registerUser(request)
        );
        assertEquals("Username is already taken!", exception.getMessage());
        verify(this.userRepository).existsByUsername(this.username, null);
        verify(this.userRepository, never()).existsByEmail(anyString(), any());
        verify(this.userMapper, never()).fromRegistrationRequest(any());
        verify(this.passwordEncoder, never()).encode(anyString());
        verify(this.userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register User with email which is already in use by another user throws UserAlreadyExistsException")
    void registerUser_duplicateEmail_throwsException() {
        String email = "test@example.com";
        String password = "Password123!";
        RegistrationRequest request = this.setupRegistationRequest(this.username, email, password);

        when(this.userRepository.existsByUsername(this.username, null)).thenReturn(false);
        when(this.userRepository.existsByEmail(email, null)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> this.userService.registerUser(request)
        );
        assertEquals("Email is already in use!", exception.getMessage());

        verify(this.userRepository).existsByUsername(this.username, null);
        verify(this.userRepository).existsByEmail(email, null);
        verify(this.userMapper, never()).fromRegistrationRequest(any());
        verify(this.passwordEncoder, never()).encode(anyString());
        verify(this.userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register User but Registration request in NULL throws IllegalArgumentException")
    void registerUser_nullRequest_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.userService.registerUser(null)
        );
        assertEquals("Registration request cannot be null", exception.getMessage());

        verify(this.userRepository, never()).existsByUsername(anyString(), anyLong());
        verify(this.userRepository, never()).existsByEmail(anyString(), anyLong());
        verify(this.userMapper, never()).fromRegistrationRequest(any());
        verify(this.passwordEncoder, never()).encode(anyString());
        verify(this.userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update User with valid data returns UserDTO")
    void updateUser_successfulUpdate() {
        Long userId = 1L;
        String newEmail = "new@example.com";
        UpdateRequest request = this.setupUpdateRequest(newEmail);
        request.setFirstName("NewFirst");
        request.setLastName("NewLast");

        User user = this.setupUser("testuser", "old@example.com", Role.USER);
        user.setId(userId);

        doAnswer(invocation -> {
            User target = invocation.getArgument(1);
            target.setEmail(request.getEmail());
            target.setFirstName(request.getFirstName());
            target.setLastName(request.getLastName());
            return null;
        }).when(this.userMapper).updateUserFromRequest(request, user);

        UserDTO userDTO = new UserDTO();
        userDTO.setFirstName("NewFirst");
        userDTO.setLastName("NewLast");
        userDTO.setEmail(newEmail);

        when(this.userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(this.userRepository.existsByEmail(newEmail, userId)).thenReturn(false);
        when(this.userRepository.save(user)).thenReturn(user);
        when(this.userMapper.toUserDTO(user)).thenReturn(userDTO);

        UserDTO response = this.userService.updateUser(userId, request);

        assertNotNull(response);
        assertSame(userDTO, response);

        verify(this.userRepository).findById(userId);
        verify(this.userRepository).existsByEmail(newEmail, userId);
        verify(this.userMapper).updateUserFromRequest(request, user);
        verify(this.userRepository).save(user);
        verify(this.userMapper).toUserDTO(user);
    }

    @Test
    @DisplayName("Update User with User ID which is not found throws ResourceNotFoundException")
    void updateUser_userNotFound_throwsException() {
        Long userId = 1L;
        UpdateRequest request = this.setupUpdateRequest("new@example.com");

        when(this.userRepository.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> this.userService.updateUser(userId, request)
        );
        assertEquals("User not found", exception.getMessage());

        verify(this.userRepository).findById(userId);
        verify(this.userRepository, never()).existsByEmail(anyString(), anyLong());
        verify(this.userMapper, never()).updateUserFromRequest(any(), any());
        verify(this.userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update User with email which is already in use by another user throws UserAlreadyExistsException")
    void updateUser_duplicateEmail_throwsException() {
        Long userId = 1L;
        String newEmail = "new@example.com";
        UpdateRequest request = this.setupUpdateRequest(newEmail);
        request.setEmail(newEmail);

        User user = this.setupUser("testuser", "old@example.com", null);
        user.setId(userId);

        when(this.userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(this.userRepository.existsByEmail(newEmail, userId)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> this.userService.updateUser(userId, request)
        );

        assertEquals("Email is already in use!", exception.getMessage());
        verify(this.userRepository).findById(userId);
        verify(this.userRepository).existsByEmail(newEmail, userId);
        verify(this.userMapper, never()).updateUserFromRequest(any(), any());
        verify(this.userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update User but userId is NULL throws IllegalArgumentException")
    void updateUser_nullUserId_throwsException() {
        UpdateRequest request = new UpdateRequest();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.userService.updateUser(null, request)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
        verify(this.userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Update User but Update request in NULL throws IllegalArgumentException")
    void updateUser_nullRequest_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.userService.updateUser(1L, null)
        );
        assertEquals("Update request cannot be null", exception.getMessage());

        verify(this.userRepository, never()).findById(anyLong());
        verify(this.userRepository, never()).existsByEmail(anyString(), anyLong());
        verify(this.userMapper, never()).updateUserFromRequest(any(), any());
        verify(this.userRepository, never()).save(any());
        verify(this.userMapper, never()).toUserDTO(any());
    }

    @Test
    @DisplayName("Change Password with valid data returns void")
    void changePassword_successfulChange() {
        Long userId = 1L;
        String oldPassword = "Password123!";
        String newPassword = "Password123!!";
        String oldHashedPassword = "oldHashedPass";
        ChangePasswordRequest request = this.setupChangePasswordRequest(oldPassword, newPassword);

        User user = this.setupUser(this.username, null, null);
        user.setId(userId);
        user.setPassword(oldHashedPassword);

        when(this.userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(this.passwordEncoder.matches(oldPassword, oldHashedPassword)).thenReturn(true);
        when(this.passwordEncoder.encode(request.getNewPassword())).thenReturn("encodedPass");
        when(this.userRepository.save(user)).thenReturn(user);

        this.userService.changePassword(userId, request);

        InOrder inOrder = inOrder(this.userRepository, this.refreshTokenRepository, this.passwordEncoder);
        inOrder.verify(this.userRepository).findById(userId);
        inOrder.verify(this.passwordEncoder).matches(oldPassword, oldHashedPassword);
        inOrder.verify(this.passwordEncoder).encode(newPassword);
        inOrder.verify(this.userRepository).save(user);
        inOrder.verify(this.refreshTokenRepository).deleteByUsername(this.username);
    }

    @Test
    @DisplayName("Change Password with incorect Old Password throws CannotProceedException")
    void changePassword_wrongOldPassword_throwsException() {
        Long userId = 1L;
        String newPassword = "Password123!!";
        ChangePasswordRequest request = this.setupChangePasswordRequest("Password123!", newPassword);

        User user = this.setupUser(this.username, null, null);
        user.setId(userId);
        user.setPassword("oldHashedPass");

        when(this.userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(this.passwordEncoder.matches(request.getOldPassword(), user.getPassword())).thenReturn(false);

        CannotProceedException exception = assertThrows(
                CannotProceedException.class,
                () -> this.userService.changePassword(userId, request)
        );
        assertEquals("Old password is incorrect", exception.getMessage());

        verify(this.userRepository).findById(userId);
        verify(this.passwordEncoder).matches(request.getOldPassword(), user.getPassword());
        verify(this.passwordEncoder, never()).encode(newPassword);
        verify(this.userRepository, never()).save(user);
        verify(this.refreshTokenRepository, never()).deleteByUsername(this.username);
    }

    @Test
    @DisplayName("Change Password but userId is NULL throws IllegalArgumentException")
    void changePassword_nullUserId_throwsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.userService.changePassword(null, request)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
        verify(this.userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Change Password but Change Password request in NULL throws IllegalArgumentException")
    void changePassword_nullRequest_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.userService.changePassword(1L, null)
        );
        assertEquals("Change Password request cannot be null", exception.getMessage());

        verify(this.userRepository, never()).findById(anyLong());
        verify(this.passwordEncoder, never()).matches(anyString(), anyString());
        verify(this.passwordEncoder, never()).encode(anyString());
        verify(this.userRepository, never()).save(any());
        verify(this.refreshTokenRepository, never()).deleteByUsername(anyString());
    }

    @Test
    @DisplayName("Get User by ID but userId is NULL throws IllegalArgumentException")
    void getUserById_nullUserId_throwsEsception() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.userService.getUserById(null)
        );

        assertEquals("User ID cannot be null", exception.getMessage());

        verify(this.userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Get User by this.username but this.username is NULL throws IllegalArgumentException")
    void getUserByUsername_nullUsername_throwsExcepton() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.userService.getUserByUsername(null)
        );

        assertEquals("Username cannot be null", exception.getMessage());

        verify(this.userRepository, never()).findByUsername(anyString());
    }
}
