package org.example.learningprojectserver;

import org.example.learningprojectserver.entities.RefreshTokenEntity;
import org.example.learningprojectserver.entities.StudentEntity;
import org.example.learningprojectserver.enums.Role;
import org.example.learningprojectserver.repository.RefreshTokenRepository;
import org.example.learningprojectserver.repository.UserRepository;
import org.example.learningprojectserver.security.IssuedTokens;
import org.example.learningprojectserver.service.AuthTokenService;
import org.example.learningprojectserver.service.AuthTokenService.RotationStatus;
import org.example.learningprojectserver.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private AuthTokenService authTokenService;

    /** Stored rows by hash, so the tests can follow a token through rotations. */
    private final Map<String, RefreshTokenEntity> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authTokenService, "refreshTtlDays", 7L);
        ReflectionTestUtils.setField(authTokenService, "reuseGraceSeconds", 30L);
        lenient().when(jwtService.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt");
        lenient().when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> {
            RefreshTokenEntity e = inv.getArgument(0);
            store.put(e.getTokenHash(), e);
            return e;
        });
        lenient().when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.<String>getArgument(0))));
        StudentEntity user = new StudentEntity();
        user.setUserId("123456789");
        user.setUsername("dana");
        user.setRole(Role.STUDENT);
        lenient().when(userRepository.findUserByUserId("123456789")).thenReturn(user);
    }

    @Test
    void issueStoresOnlyTheHashOfTheRefreshToken() {
        IssuedTokens tokens = authTokenService.issue("123456789", "dana", "STUDENT");

        assertEquals("jwt", tokens.accessToken());
        assertEquals(1, store.size());
        RefreshTokenEntity stored = store.values().iterator().next();
        assertNotEquals(tokens.refreshToken(), stored.getTokenHash());
        assertEquals(64, stored.getTokenHash().length());
        assertTrue(stored.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void rotateRevokesOldTokenAndIssuesNewOneInSameFamily() {
        IssuedTokens first = authTokenService.issue("123456789", "dana", "STUDENT");

        AuthTokenService.RotationResult result = authTokenService.rotate(first.refreshToken());

        assertEquals(RotationStatus.ROTATED, result.status());
        assertNotEquals(first.refreshToken(), result.tokens().refreshToken());
        assertEquals(2, store.size());
        long revoked = store.values().stream().filter(e -> e.getRevokedAt() != null).count();
        assertEquals(1, revoked);
        assertEquals(1, store.values().stream().map(RefreshTokenEntity::getFamilyId).distinct().count());
    }

    @Test
    void reuseWithinGraceIsAlreadyRotatedWithoutRevokingTheFamily() {
        IssuedTokens first = authTokenService.issue("123456789", "dana", "STUDENT");
        authTokenService.rotate(first.refreshToken());

        AuthTokenService.RotationResult again = authTokenService.rotate(first.refreshToken());

        assertEquals(RotationStatus.ALREADY_ROTATED, again.status());
        verify(refreshTokenRepository, never()).revokeFamily(anyString(), any());
    }

    @Test
    void reuseAfterGraceRevokesTheWholeFamily() {
        IssuedTokens first = authTokenService.issue("123456789", "dana", "STUDENT");
        authTokenService.rotate(first.refreshToken());
        store.values().stream().filter(e -> e.getRevokedAt() != null)
                .forEach(e -> e.setRevokedAt(Instant.now().minusSeconds(120)));

        AuthTokenService.RotationResult reuse = authTokenService.rotate(first.refreshToken());

        assertEquals(RotationStatus.INVALID, reuse.status());
        ArgumentCaptor<String> family = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenRepository).revokeFamily(family.capture(), any());
        assertEquals(store.values().iterator().next().getFamilyId(), family.getValue());
    }

    @Test
    void unknownExpiredOrMissingTokensAreInvalid() {
        assertEquals(RotationStatus.INVALID, authTokenService.rotate(null).status());
        assertEquals(RotationStatus.INVALID, authTokenService.rotate("not-a-token").status());

        IssuedTokens tokens = authTokenService.issue("123456789", "dana", "STUDENT");
        store.values().forEach(e -> e.setExpiresAt(Instant.now().minusSeconds(1)));
        assertEquals(RotationStatus.INVALID, authTokenService.rotate(tokens.refreshToken()).status());
    }
}
