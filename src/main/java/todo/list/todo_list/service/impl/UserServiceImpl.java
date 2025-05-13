package todo.list.todo_list.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import todo.list.todo_list.dto.Admin.AdminUserCreationRequest;
import todo.list.todo_list.dto.Admin.AdminUserCreationResponse;
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
import todo.list.todo_list.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_PASSWORD_CHANGE_ATTEMPTS = 3;
    private static final long PASSWORD_CHANGE_WINDOW_SECONDS = 60;
    private static final Map<Long, List<Long>> PASSWORD_CHANGE_ATTEMPTS = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, RefreshTokenRepository refreshTokenRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userMapper = userMapper;
    }

    @Override
    public RegistrationResponse registerUser(@Valid RegistrationRequest request) {
        log.debug("Received Register User request");
        validateRegistrationRequest(request);

        String username = request.getUsername();
        String email = request.getEmail();

        if (username.length() < MIN_USERNAME_LENGTH) {
            log.warn("Short username detected: {}", username);
        }
        if (email.length() > MAX_EMAIL_LENGTH) {
            log.warn("Long email detected: {}", email);
        }

        if (userRepository.existsByUsername(username, null)) {
            throw new UserAlreadyExistsException("Username is already taken!");
        }
        if (userRepository.existsByEmail(email, null)) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        User user = userMapper.fromRegistrationRequest(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        User savedUser = userRepository.save(user);

        log.info("Successfully registered user and assigned userID: {}", savedUser.getId());
        return new RegistrationResponse("User registered successfully", user.getUsername(), user.getEmail());
    }

    @Override
    public UserDTO updateUser(Long userId, @Valid UpdateRequest request) {
        log.debug("Received UPDATE User request with userID: {}", userId);
        validateUserId(userId);
        validateUpdateRequest(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        String email = request.getEmail();
        if (email != null && email.length() > MAX_EMAIL_LENGTH) {
            log.warn("Long email detected: {}", email);
        }

        if (email != null && userRepository.existsByEmail(email, userId)) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        userMapper.updateUserFromRequest(request, user);
        User updatedUser = userRepository.save(user);
        log.info("Successfully updated user with userID: {}", userId);
        return userMapper.toUserDTO(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.debug("Received Change Password request for user with userID: {}", userId);
        validateUserId(userId);
        validateChangePasswordRequest(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new CannotProceedException("Old password is incorrect");
        }

        trackPasswordChangeAttempt(userId);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.deleteByUsername(user.getUsername());
        log.info("Successfully changed password for user with userID: {}", userId);
    }

    @Override
    public User getUserById(Long userId) {
        log.debug("Received request to get user by userID: {}", userId);
        validateUserId(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        log.info("Successfully retrieved user with userID: {}", userId);
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        log.debug("Received request to get user by username: {}", username);
        validateUsername(username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        log.info("Successfully retrieved user with username: {}", username);
        return user;
    }

    @Override
    public AdminUserCreationResponse createUserWithAdminOrModeratorRole(@Valid AdminUserCreationRequest request) {
        log.debug("Received Admin User Creation request");
        validateCreationRequest(request);

        String username = request.getUsername();
        String email = request.getEmail();

        if (username.length() < MIN_USERNAME_LENGTH) {
            log.warn("Short username detected: {}", username);
        }
        if (email.length() > MAX_EMAIL_LENGTH) {
            log.warn("Long email detected: {}", email);
        }

        if (userRepository.existsByUsername(username, null)) {
            throw new UserAlreadyExistsException("Username is already taken!");
        }
        if (userRepository.existsByEmail(email, null)) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        User user = userMapper.fromAdminUserCreationRequest(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        log.info("Successfully created user: {} with role: {}", savedUser.getUsername(), savedUser.getRole());
        return new AdminUserCreationResponse("User created successfully", savedUser.getUsername(), savedUser.getEmail(), savedUser.getRole());
    }

    private void validateRegistrationRequest(RegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }
    }

    private void validateCreationRequest(AdminUserCreationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }
    }

    private void validateUpdateRequest(UpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Update request cannot be null");
        }
    }

    private void validateChangePasswordRequest(ChangePasswordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Change Password request cannot be null");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
    }

    private void validateUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
    }

    private void trackPasswordChangeAttempt(Long userId) {
        long currentTime = Instant.now().getEpochSecond();
        List<Long> attempts = PASSWORD_CHANGE_ATTEMPTS.computeIfAbsent(userId, k -> new ArrayList<>());

        attempts.removeIf(timestamp -> currentTime - timestamp > PASSWORD_CHANGE_WINDOW_SECONDS);

        attempts.add(currentTime);

        if (attempts.size() > MAX_PASSWORD_CHANGE_ATTEMPTS) {
            log.warn("Frequent password change attempts detected for user ID: {}, attempts: {}", userId, attempts.size());
        }
    }
}
