package todo.list.todo_list.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import todo.list.todo_list.entity.RefreshToken;
import todo.list.todo_list.entity.User;
import todo.list.todo_list.repository.RefreshTokenRepository;
import todo.list.todo_list.security.JwtUtil;
import todo.list.todo_list.service.RefreshTokenService;
import todo.list.todo_list.service.UserService;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, JwtUtil jwtUtil, UserService userService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    public String generateNewAccessToken(String refreshToken) {
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token cannot be null");
        }
        
        Optional<RefreshToken> storedRefreshToken = refreshTokenRepository.findByRefreshToken(refreshToken);

        if (storedRefreshToken.isEmpty() || storedRefreshToken.get().getExpiration().isBefore(Instant.now())) {
            return null;
        }

        User user = userService.getUserByUsername(storedRefreshToken.get().getUsername());
        List<String> roles = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList());

        return jwtUtil.generateAccessToken(user.getUsername(), roles);
    }

    @Override
    public RefreshToken createRefreshToken(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsername(username);
        refreshToken.setRefreshToken(jwtUtil.generateRefreshToken(username));
        refreshToken.setExpiration(Instant.now().plusMillis(jwtUtil.getRefreshExpirationMillis()));

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void deleteByUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        refreshTokenRepository.deleteByUsername(username);
    }
}
