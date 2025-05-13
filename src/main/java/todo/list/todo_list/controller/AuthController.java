package todo.list.todo_list.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import todo.list.todo_list.dto.Admin.AdminUserCreationRequest;
import todo.list.todo_list.dto.Admin.AdminUserCreationResponse;
import todo.list.todo_list.dto.Auth.AuthRequest;
import todo.list.todo_list.dto.Auth.AuthResponse;
import todo.list.todo_list.dto.Auth.RefreshTokenRequest;
import todo.list.todo_list.dto.Registration.RegistrationRequest;
import todo.list.todo_list.dto.Registration.RegistrationResponse;
import todo.list.todo_list.service.AuthService;
import todo.list.todo_list.service.UserService;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        log.debug("Received registration request for username: {}", request.getUsername());
        RegistrationResponse response = userService.registerUser(request);
        log.info("Successfully registered user: {}", request.getUsername());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        log.debug("Received login request for username: {}", authRequest.getUsername());
        AuthResponse response = authService.authenticate(authRequest);
        log.info("User logged in successfully: {}", authRequest.getUsername());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        log.debug("Received token refresh request for token: {}",
                refreshTokenRequest.getRefreshToken() != null
                ? refreshTokenRequest.getRefreshToken().substring(0, Math.min(10, refreshTokenRequest.getRefreshToken().length())) + "..." : null);
        AuthResponse response = authService.refreshToken(refreshTokenRequest.getRefreshToken());
        log.info("Token refreshed successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        log.debug("Received logout request for token: {}",
                refreshTokenRequest.getRefreshToken() != null
                ? refreshTokenRequest.getRefreshToken().substring(0, Math.min(10, refreshTokenRequest.getRefreshToken().length())) + "..." : null);
        authService.logout(refreshTokenRequest.getRefreshToken());
        log.info("User logged out successfully");

        return ResponseEntity.ok("Logged out successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/register")
    public ResponseEntity<AdminUserCreationResponse> createUserWithAdminOrModeratorRole(@Valid @RequestBody AdminUserCreationRequest request) {
        log.debug("Received admin user creation request for username: {}", request.getUsername());
        AdminUserCreationResponse response = userService.createUserWithAdminOrModeratorRole(request);
        log.info("Successfully created user: {} with role: {}", request.getUsername(), request.getRole());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.debug("Received DELETE User request by userID: {}", userId);

        userService.deleteUser(userId);
        log.info("Successfully deleted User by userID: {}", userId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
