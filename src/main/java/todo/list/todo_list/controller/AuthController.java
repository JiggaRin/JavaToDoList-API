package todo.list.todo_list.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import todo.list.todo_list.dto.Auth.AuthRequest;
import todo.list.todo_list.dto.Auth.AuthResponse;
import todo.list.todo_list.dto.RegistrationRequest;
import todo.list.todo_list.security.JwtUtil;
import todo.list.todo_list.service.AuthService;
import todo.list.todo_list.service.RefreshTokenService;
import todo.list.todo_list.service.UserService;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UserService userService, AuthService authService, JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        userService.registerUser(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        return ResponseEntity.ok(authService.authenticate(authRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        String newAccessToken = refreshTokenService.generateNewAccessToken(refreshToken);

        if (newAccessToken == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Invalid or expired refresh token"));
        }

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(403).body(Map.of("error", "Invalid refresh token"));
        }
        String username = jwtUtil.extractUsername(refreshToken);
        refreshTokenService.deleteByUsername(username);
        return ResponseEntity.status(200).body(Map.of("message", "Logged out successfully"));
    }
}