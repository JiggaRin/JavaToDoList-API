package todo.list.todo_list.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
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
        try {
            ResponseEntity<RegistrationResponse> response = this.registerUser(request);

            return response;
        } catch (Exception e) {
            log.error("Registration failed for username: {}", request.getUsername(), e);
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        log.debug("Received login request for username: {}", authRequest.getUsername());
        try {
            ResponseEntity<AuthResponse> response = this.loginUser(authRequest);

            return response;
        } catch (Exception e) {
            log.error("Login failed for username: {}", authRequest.getUsername(), e);
            throw e;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        log.debug("Received token refresh request");
        try {
            ResponseEntity<AuthResponse> response = this.refreshToken(refreshTokenRequest);

            return response;
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        log.debug("Received logout request");
        try {
            ResponseEntity<String> response = this.logoutUser(refreshTokenRequest);

            return response;
        } catch (Exception e) {
            log.error("Logout failed", e);
            throw e;
        }
    }

    private ResponseEntity<RegistrationResponse> registerUser(RegistrationRequest request) {
        RegistrationResponse response = userService.registerUser(request);
        log.info("Successfully registered user: {}", request.getUsername());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    private ResponseEntity<AuthResponse> loginUser(AuthRequest authRequest) {
        AuthResponse response = authService.authenticate(authRequest);
        log.info("User logged in successfully: {}", authRequest.getUsername());

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<AuthResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        AuthResponse response = authService.refreshToken(refreshTokenRequest.getRefreshToken());
        log.info("Token refreshed successfully");

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<String> logoutUser(RefreshTokenRequest refreshTokenRequest) {
        authService.logout(refreshTokenRequest.getRefreshToken());
        log.info("User logged out successfully");

        return ResponseEntity.ok("Logged out successfully");
    }
}