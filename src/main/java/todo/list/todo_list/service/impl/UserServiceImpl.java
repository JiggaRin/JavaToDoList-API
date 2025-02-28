package todo.list.todo_list.service.impl;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
import todo.list.todo_list.repository.UserRepository;
import todo.list.todo_list.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegistrationResponse registerUser(@Valid RegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername(), null)) {
            throw new UserAlreadyExistsException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail(), null)) {
            throw new UserAlreadyExistsException("Email is already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        userRepository.save(user);

        return new RegistrationResponse("User registered successfully", user.getUsername(), user.getEmail());
    }

    @Override
    public UserDTO updateUser(Long userId, @Valid UpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional.ofNullable(request.getEmail())
                .filter(email -> !userRepository.existsByEmail(email, user.getId()))
                .ifPresentOrElse(
                        user::setEmail,
                        () -> {
                            if (request.getEmail() != null) {
                                throw new UserAlreadyExistsException("Email is already taken");

                            }
                        }
                );

        Optional.ofNullable(request.getFirstName()).ifPresent(user::setFirstName);
        Optional.ofNullable(request.getLastName()).ifPresent(user::setLastName);

        return new UserDTO(userRepository.save(user));
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new CannotProceedException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
}
