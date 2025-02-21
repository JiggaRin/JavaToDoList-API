package todo.list.todo_list.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import todo.list.todo_list.dto.Auth.AuthRequest;
import todo.list.todo_list.dto.Auth.AuthResponse;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.security.JwtUtil;
import todo.list.todo_list.service.AuthService;
import todo.list.todo_list.service.RefreshTokenService;
import todo.list.todo_list.service.UserService;

@Service
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    public AuthServiceImpl(JwtUtil jwtUtil, RefreshTokenService refreshTokenService, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
    }

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) {
        User user = userService.getUserByUsername(authRequest.getUsername());
        List<String> roles = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList());
        String accessToken = jwtUtil.generateAccessToken(authRequest.getUsername(), roles);
        String refreshToken = refreshTokenService.createRefreshToken(authRequest.getUsername()).getRefreshToken();

        return new AuthResponse(accessToken, refreshToken);
    }
}
