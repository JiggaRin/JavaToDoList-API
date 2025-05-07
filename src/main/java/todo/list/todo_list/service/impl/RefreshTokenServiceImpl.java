package todo.list.todo_list.service.impl;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import todo.list.todo_list.entity.RefreshToken;
import todo.list.todo_list.repository.RefreshTokenRepository;
import todo.list.todo_list.security.JwtUtil;
import todo.list.todo_list.service.RefreshTokenService;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);
    private static final int MIN_USERNAME_LENGTH = 3;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, JwtUtil jwtUtil) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public RefreshToken createRefreshToken(String username) {
        log.debug("Received Create Refresh Token request for username: {}", username);
        validateUsername(username);

        if (username.length() < MIN_USERNAME_LENGTH) {
            log.warn("Short username detected: {}", username);
        }

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsername(username);
        refreshToken.setRefreshToken(jwtUtil.generateRefreshToken(username));
        refreshToken.setExpiration(Instant.now().plusMillis(jwtUtil.getRefreshExpirationMillis()));

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        log.info("Successfully created Refresh Token for username: {}", username);
        return savedToken;
    }

    @Override
    @Transactional
    public void deleteByUsername(String username) {
        log.debug("Received Delete refresh tokens request for username: {}", username);
        validateUsername(username);

        refreshTokenRepository.deleteByUsername(username);
        log.info("Successfully deleted Refresh Tokens for username: {}", username);
    }

    private void validateUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
    }
}
