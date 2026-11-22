package org.example.learningprojectserver.service;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.entities.RefreshTokenEntity;
import org.example.learningprojectserver.entities.UserEntity;
import org.example.learningprojectserver.repository.RefreshTokenRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.security.IssuedTokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues access + refresh tokens and rotates refresh tokens.
 * Refresh tokens are random 256-bit values; only their SHA-256 hash is stored (table refresh_tokens).
 */
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    public enum RotationStatus { ROTATED, ALREADY_ROTATED, INVALID }

    public record RotationResult(RotationStatus status, IssuedTokens tokens) {
        static RotationResult invalid() {
            return new RotationResult(RotationStatus.INVALID, null);
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${auth.refresh-token.ttl-days:7}")
    private long refreshTtlDays;

    /**
     * How long after a rotation the old token is answered with ALREADY_ROTATED instead of being
     * treated as reuse. Covers two tabs refreshing at once: the browser already holds the new cookies.
     */
    @Value("${auth.refresh-token.reuse-grace-seconds:30}")
    private long reuseGraceSeconds;

    /** Starts a new token family (a new login). */
    @Transactional
    public IssuedTokens issue(String userId, String username, String role) {
        String familyId = UUID.randomUUID().toString();
        return new IssuedTokens(
                jwtService.generateToken(userId, username, role),
                storeNewRefreshToken(userId, familyId));
    }

    /** Revokes the presented refresh token and issues a new pair in the same family. */
    @Transactional
    public RotationResult rotate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return RotationResult.invalid();
        }
        Optional<RefreshTokenEntity> found = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken));
        if (found.isEmpty()) {
            return RotationResult.invalid();
        }
        RefreshTokenEntity current = found.get();
        Instant now = Instant.now();

        if (current.getRevokedAt() != null) {
            if (current.getRevokedAt().isAfter(now.minusSeconds(reuseGraceSeconds))) {
                return new RotationResult(RotationStatus.ALREADY_ROTATED, null);
            }
            // A revoked token presented again: assume it was stolen and end the whole session family.
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            return RotationResult.invalid();
        }
        if (current.getExpiresAt().isBefore(now)) {
            return RotationResult.invalid();
        }

        UserEntity user = userRepository.findUserByUserId(current.getUserId());
        if (user == null) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            return RotationResult.invalid();
        }

        current.setRevokedAt(now);
        refreshTokenRepository.save(current);

        IssuedTokens tokens = new IssuedTokens(
                jwtService.generateToken(user.getUserId(), user.getUsername(), String.valueOf(user.getRole())),
                storeNewRefreshToken(user.getUserId(), current.getFamilyId()));
        return new RotationResult(RotationStatus.ROTATED, tokens);
    }

    /** Logout: revokes the family of the presented refresh token. Unknown tokens are ignored. */
    @Transactional
    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), Instant.now()));
    }

    /** Revokes every refresh token of a user (used after a password reset). */
    @Transactional
    public void revokeAllForUser(String userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Jerusalem")
    @Transactional
    public void purgeExpired() {
        refreshTokenRepository.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(1)));
    }

    private String storeNewRefreshToken(String userId, String familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant now = Instant.now();
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setTokenHash(hash(raw));
        entity.setFamilyId(familyId);
        entity.setUserId(userId);
        entity.setCreatedAt(now);
        entity.setExpiresAt(now.plus(Duration.ofDays(refreshTtlDays)));
        refreshTokenRepository.save(entity);
        return raw;
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
