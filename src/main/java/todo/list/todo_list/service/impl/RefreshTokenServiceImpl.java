package todo.list.todo_list.service.impl;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import todo.list.todo_list.entity.RefreshToken;
import todo.list.todo_list.repository.RefreshTokenRepository;
import todo.list.todo_list.security.JwtUtil;
import todo.list.todo_list.service.RefreshTokenService;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, JwtUtil jwtUtil) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String generateNewAccessToken(String refreshToken) {
        Optional<RefreshToken> storedRefreshToken = refreshTokenRepository.findByRefreshToken(refreshToken);

        if (storedRefreshToken.isEmpty() || storedRefreshToken.get().getExpiration().isBefore(Instant.now())) {
            return null;
        }

        String username = storedRefreshToken.get().getUsername();
        return jwtUtil.generateAccessToken(username);
    }

    @Override
    public RefreshToken createRefreshToken(String username) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsername(username);
        refreshToken.setRefreshToken(jwtUtil.generateRefreshToken(username));
        refreshToken.setExpiration(Instant.now().plusMillis(jwtUtil.getRefreshExpirationMillis()));

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public void deleteByUsername(String username) {
        refreshTokenRepository.deleteByUsername(username);
    }
}
