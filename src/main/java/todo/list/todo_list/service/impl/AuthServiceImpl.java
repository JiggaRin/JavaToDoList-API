package todo.list.todo_list.service.impl;

import org.springframework.stereotype.Service;

import todo.list.todo_list.dto.Auth.AuthRequest;
import todo.list.todo_list.dto.Auth.AuthResponse;
import todo.list.todo_list.security.JwtUtil;
import todo.list.todo_list.service.AuthService;
import todo.list.todo_list.service.RefreshTokenService;

@Service
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) {
        String accessToken = jwtUtil.generateAccessToken(authRequest.getUsername());
        String refreshToken = refreshTokenService.createRefreshToken(authRequest.getUsername()).getRefreshToken();

        return new AuthResponse(accessToken, refreshToken);
    }
}
