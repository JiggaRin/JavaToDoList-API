package todo.list.todo_list.service.impl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";
    private static final String EMAIL = "test@example.com";
    private static final String NEW_EMAIL = "new@example.com";
    private static final String PASSWORD = "Password123!";
    private static final String NEW_PASSWORD = "Password123!!";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String OLD_HASHED_PASSWORD = "oldHashedPass";
    private static final String NEW_ENCODED_PASSWORD = "encodedPass";
    private static final String FIRST_NAME = "Test";
    private static final String LAST_NAME = "User";
    private static final String NEW_FIRST_NAME = "NewFirst";
    private static final String NEW_LAST_NAME = "NewLast";

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

    private User defaultUser;

    @BeforeEach
    @SuppressWarnings("unused")
    void setup() {
        defaultUser = new User(USERNAME, OLD_HASHED_PASSWORD, Role.USER);
        defaultUser.setId(USER_ID);
        defaultUser.setEmail(EMAIL);
        defaultUser.setFirstName(FIRST_NAME);
        defaultUser.setLastName(LAST_NAME);
    }

    private RegistrationRequest setupRegistrationRequest(String username, String email, String password) {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setFirstName(FIRST_NAME);
        request.setLastName(LAST_NAME);
        request.setRole(Role.USER);
        return request;
    }

    private UpdateRequest setupUpdateRequest(String newEmail, String firstName, String lastName) {
        UpdateRequest request = new UpdateRequest();
        request.setEmail(newEmail);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        return request;
    }

    private ChangePasswordRequest setupChangePasswordRequest(String oldPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword(oldPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    private UserDTO setupUserDTO(String email, String firstName, String lastName) {
        UserDTO dto = new UserDTO();
        dto.setEmail(email);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        return dto;
    }

    private void setupSuccessfulRegistrationMocks(RegistrationRequest request, User user) {
        when(userRepository.existsByUsername(USERNAME, null)).thenReturn(false);
        when(userRepository.existsByEmail(EMAIL, null)).thenReturn(false);
        when(userMapper.fromRegistrationRequest(request)).thenReturn(user);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(user);
    }

    private void setupSuccessfulUpdateMocks(User user, UserDTO dto) {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(defaultUser));
        when(userRepository.existsByEmail(NEW_EMAIL, USER_ID)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserDTO(any(User.class))).thenReturn(dto);
    }

    private void setupSuccessfulChangePasswordMocks() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(defaultUser));
        when(passwordEncoder.matches(PASSWORD, OLD_HASHED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(defaultUser);
    }

    @Test
    @DisplayName("Register User with valid data returns RegistrationResponse")
    void registerUser_successfulRegistration() {
        // Arrange
        RegistrationRequest request = setupRegistrationRequest(USERNAME, EMAIL, PASSWORD);
        User user = new User(USERNAME, ENCODED_PASSWORD, Role.USER);
        user.setEmail(EMAIL);
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        setupSuccessfulRegistrationMocks(request, user);

        // Act
        RegistrationResponse response = userService.registerUser(request);

        // Assert
        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        assertEquals(USERNAME, response.getUsername());
        assertEquals(EMAIL, response.getEmail());
        verify(userRepository).existsByUsername(USERNAME, null);
        verify(userRepository).existsByEmail(EMAIL, null);
        verify(userMapper).fromRegistrationRequest(request);
        verify(passwordEncoder).encode(PASSWORD);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register User with duplicate username throws UserAlreadyExistsException")
    void registerUser_duplicateUsername_throwsException() {
        // Arrange
        RegistrationRequest request = setupRegistrationRequest(USERNAME, EMAIL, PASSWORD);
        when(userRepository.existsByUsername(USERNAME, null)).thenReturn(true);

        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.registerUser(request)
        );
        assertEquals("Username is already taken!", exception.getMessage());
        verify(userRepository).existsByUsername(USERNAME, null);
        verify(userRepository, never()).existsByEmail(anyString(), any());
        verifyNoUserRegister();
    }

    @Test
    @DisplayName("Register User with duplicate email throws UserAlreadyExistsException")
    void registerUser_duplicateEmail_throwsException() {
        // Arrange
        RegistrationRequest request = setupRegistrationRequest(USERNAME, EMAIL, PASSWORD);
        when(userRepository.existsByUsername(USERNAME, null)).thenReturn(false);
        when(userRepository.existsByEmail(EMAIL, null)).thenReturn(true);

        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.registerUser(request)
        );
        assertEquals("Email is already in use!", exception.getMessage());
        verify(userRepository).existsByUsername(USERNAME, null);
        verify(userRepository).existsByEmail(EMAIL, null);
        verifyNoUserRegister();
    }

    @Test
    @DisplayName("Register User with null request throws IllegalArgumentException")
    void registerUser_nullRequest_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(null)
        );
        assertEquals("Registration request cannot be null", exception.getMessage());
        verify(userRepository, never()).existsByUsername(anyString(), anyLong());
        verify(userRepository, never()).existsByEmail(anyString(), anyLong());
        verifyNoUserRegister();
    }

    @Test
    @DisplayName("Get User by ID with null ID throws IllegalArgumentException")
    void getUserById_nullUserId_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(null)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
        verify(userMapper, never()).toUserDTO(any());
    }

    @Test
    @DisplayName("Get User by username with null username throws IllegalArgumentException")
    void getUserByUsername_nullUsername_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserByUsername(null)
        );
        assertEquals("Username cannot be null", exception.getMessage());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Update User with valid data returns UserDTO")
    void updateUser_successfulUpdate() {
        // Arrange
        UpdateRequest request = setupUpdateRequest(NEW_EMAIL, NEW_FIRST_NAME, NEW_LAST_NAME);
        User updatedUser = new User(USERNAME, OLD_HASHED_PASSWORD, Role.USER);
        updatedUser.setId(USER_ID);
        updatedUser.setEmail(NEW_EMAIL);
        updatedUser.setFirstName(NEW_FIRST_NAME);
        updatedUser.setLastName(NEW_LAST_NAME);
        UserDTO dto = setupUserDTO(NEW_EMAIL, NEW_FIRST_NAME, NEW_LAST_NAME);
        setupSuccessfulUpdateMocks(updatedUser, dto);

        // Act
        UserDTO response = userService.updateUser(USER_ID, request);

        // Assert
        assertNotNull(response);
        assertEquals(NEW_EMAIL, response.getEmail());
        assertEquals(NEW_FIRST_NAME, response.getFirstName());
        assertEquals(NEW_LAST_NAME, response.getLastName());
        verify(userRepository).findById(USER_ID);
        verify(userRepository).existsByEmail(NEW_EMAIL, USER_ID);
        verify(userRepository).save(any(User.class));
        verify(userMapper).toUserDTO(any(User.class));
    }

    @Test
    @DisplayName("Update User with non-existent ID throws ResourceNotFoundException")
    void updateUser_userNotFound_throwsException() {
        // Arrange
        UpdateRequest request = setupUpdateRequest(NEW_EMAIL, NEW_FIRST_NAME, NEW_LAST_NAME);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.updateUser(USER_ID, request)
        );
        assertEquals("User not found with ID: " + USER_ID, exception.getMessage());
        verify(userRepository).findById(USER_ID);
        verify(userRepository, never()).existsByEmail(anyString(), anyLong());
        verifyNoUserUpdate();
    }

    @Test
    @DisplayName("Update User with duplicate email throws UserAlreadyExistsException")
    void updateUser_duplicateEmail_throwsException() {
        // Arrange
        UpdateRequest request = setupUpdateRequest(NEW_EMAIL, NEW_FIRST_NAME, NEW_LAST_NAME);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(defaultUser));
        when(userRepository.existsByEmail(NEW_EMAIL, USER_ID)).thenReturn(true);

        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.updateUser(USER_ID, request)
        );
        assertEquals("Email is already in use!", exception.getMessage());
        verify(userRepository).findById(USER_ID);
        verify(userRepository).existsByEmail(NEW_EMAIL, USER_ID);
        verifyNoUserUpdate();
    }

    @Test
    @DisplayName("Update User with null ID throws IllegalArgumentException")
    void updateUser_nullUserId_throwsException() {
        // Arrange
        UpdateRequest request = setupUpdateRequest(NEW_EMAIL, NEW_FIRST_NAME, NEW_LAST_NAME);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(null, request)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).existsByEmail(anyString(), anyLong());
        verifyNoUserUpdate();
    }

    @Test
    @DisplayName("Update User with null request throws IllegalArgumentException")
    void updateUser_nullRequest_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(USER_ID, null)
        );
        assertEquals("Update request cannot be null", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).existsByEmail(anyString(), anyLong());
        verifyNoUserUpdate();
    }

    @Test
    @DisplayName("Change Password with valid data succeeds")
    void changePassword_successfulChange() {
        // Arrange
        ChangePasswordRequest request = setupChangePasswordRequest(PASSWORD, NEW_PASSWORD);
        setupSuccessfulChangePasswordMocks();

        // Act
        userService.changePassword(USER_ID, request);

        // Assert
        InOrder inOrder = inOrder(userRepository, refreshTokenRepository, passwordEncoder);
        inOrder.verify(userRepository).findById(USER_ID);
        inOrder.verify(passwordEncoder).matches(PASSWORD, OLD_HASHED_PASSWORD);
        inOrder.verify(passwordEncoder).encode(NEW_PASSWORD);
        inOrder.verify(userRepository).save(any(User.class));
        inOrder.verify(refreshTokenRepository).deleteByUsername(USERNAME);
    }

    @Test
    @DisplayName("Change Password with incorrect old password throws CannotProceedException")
    void changePassword_wrongOldPassword_throwsException() {
        // Arrange
        ChangePasswordRequest request = setupChangePasswordRequest(PASSWORD, NEW_PASSWORD);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(defaultUser));
        when(passwordEncoder.matches(PASSWORD, OLD_HASHED_PASSWORD)).thenReturn(false);

        // Act & Assert
        CannotProceedException exception = assertThrows(
                CannotProceedException.class,
                () -> userService.changePassword(USER_ID, request)
        );
        assertEquals("Old password is incorrect", exception.getMessage());
        verify(userRepository).findById(USER_ID);
        verify(passwordEncoder).matches(PASSWORD, OLD_HASHED_PASSWORD);
        verifyNoUserChangePassword();
    }

    @Test
    @DisplayName("Change Password with null ID throws IllegalArgumentException")
    void changePassword_nullUserId_throwsException() {
        // Arrange
        ChangePasswordRequest request = setupChangePasswordRequest(PASSWORD, NEW_PASSWORD);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.changePassword(null, request)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
        verifyNoUserChangePassword();
    }

    @Test
    @DisplayName("Change Password with null request throws IllegalArgumentException")
    void changePassword_nullRequest_throwsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.changePassword(USER_ID, null)
        );
        assertEquals("Change Password request cannot be null", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
        verifyNoUserChangePassword();
    }

    private void verifyNoUserRegister() {
        verify(userMapper, never()).fromRegistrationRequest(any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    private void verifyNoUserUpdate() {
        verify(userMapper, never()).updateUserFromRequest(any(), any());
        verify(userRepository, never()).save(any(User.class));
        verify(userMapper, never()).toUserDTO(any(User.class));
    }

    private void verifyNoUserChangePassword() {
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(refreshTokenRepository, never()).deleteByUsername(anyString());
    }
}