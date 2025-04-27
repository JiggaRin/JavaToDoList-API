package todo.list.todo_list.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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
import todo.list.todo_list.repository.RefreshTokenRepository;
import todo.list.todo_list.repository.UserRepository;
import todo.list.todo_list.service.UserService;

@Service
public class UserServiceImpl implements UserService {

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
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }
        if (this.userRepository.existsByUsername(request.getUsername(), null)) {
            throw new UserAlreadyExistsException("Username is already taken!");
        }
        if (this.userRepository.existsByEmail(request.getEmail(), null)) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        User user = this.userMapper.fromRegistrationRequest(request);
        user.setPassword(this.passwordEncoder.encode(request.getPassword()));

        this.userRepository.save(user);

        return new RegistrationResponse("User registered successfully", user.getUsername(), user.getEmail());
    }

    @Override
    public UserDTO updateUser(Long userId, @Valid UpdateRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (request == null) {
            throw new IllegalArgumentException("Update request cannot be null");
        }
        User user = this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (this.userRepository.existsByEmail(request.getEmail(), userId)) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        this.userMapper.updateUserFromRequest(request, user);
        return this.userMapper.toUserDTO(this.userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (request == null) {
            throw new IllegalArgumentException("Change Password request cannot be null");
        }

        User user = this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!this.passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new CannotProceedException("Old password is incorrect");
        }

        user.setPassword(this.passwordEncoder.encode(request.getNewPassword()));
        this.userRepository.save(user);
        this.refreshTokenRepository.deleteByUsername(user.getUsername());
    }

    @Override
    public User getUserById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    @Override
    public User getUserByUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        
        return this.userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
}
