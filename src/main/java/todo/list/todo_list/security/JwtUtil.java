package todo.list.todo_list.security;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refreshExpiration}")
    private long refreshExpiration;

    public String generateAccessToken(String username, List<String> roles) {
        log.debug("Received Generate Access Token request for username: {}", username);
        validateUsername(username);

        String token = JWT.create()
                .withSubject(username)
                .withClaim("roles", roles)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expiration * 1000))
                .sign(Algorithm.HMAC256(secret));

        log.info("Successfully generated Access Token for username: {}", username);
        return token;
    }

    public String generateRefreshToken(String username) {
        log.debug("Received Generate Refresh Token request for username: {}", username);
        validateUsername(username);

        String token = JWT.create()
                .withSubject(username)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + refreshExpiration * 1000))
                .sign(Algorithm.HMAC256(secret));

        log.info("Successfully generated Refresh Token for username: {}", username);
        return token;
    }

    public String extractUsername(String token) {
        log.debug("Received request to extract username from token");
        validateToken(token);

        try {
            String username = JWT.require(Algorithm.HMAC256(secret))
                    .build()
                    .verify(token)
                    .getSubject();
            log.info("Successfully extracted username from token");
            return username;
        } catch (JWTVerificationException e) {
            log.warn("Invalid token detected during username extraction: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        log.debug("Received Validate Token request");
        validateRefreshToken(token);

        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
            DecodedJWT jwt = verifier.verify(token);
            boolean isValid = jwt.getExpiresAt().after(new Date());
            if (isValid) {
                log.info("Successfully validated Token");
            } else {
                log.warn("Expired Token detected during Validation");
            }
            return isValid;
        } catch (JWTVerificationException e) {
            log.warn("Invalid Token detected during Validation: {}", e.getMessage());
            return false;
        }
    }

    public List<String> extractRoles(String token) {
        log.debug("Received Extract Roles from Token request");
        validateRefreshToken(token);

        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret))
                    .build()
                    .verify(token);
            List<String> roles = jwt.getClaim("roles").asList(String.class);
            log.info("Successfully extracted Roles from Token");
            return roles;
        } catch (JWTVerificationException e) {
            log.warn("Invalid Token detected during Roles Extraction: {}", e.getMessage());
            return null;
        }
    }

    public DecodedJWT verifyToken(String token) {
        log.debug("Received Verify Token request");
        validateRefreshToken(token);

        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
            DecodedJWT jwt = verifier.verify(token);
            log.info("Successfully verified Token");
            return jwt;
        } catch (JWTVerificationException e) {
            log.warn("Invalid Token detected during Verification: {}", e.getMessage());
            return null;
        }
    }

    public long getRefreshExpirationMillis() {
        log.debug("Received Get Refresh Token Expiration request");
        log.info("Successfully retrieved Refresh Token Expiration: {} ms", refreshExpiration * 1000);
        return refreshExpiration * 1000;
    }

    private void validateUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
    }

    private void validateRefreshToken(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }
    }
}
