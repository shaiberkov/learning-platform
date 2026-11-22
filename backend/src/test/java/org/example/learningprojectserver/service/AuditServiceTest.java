package org.example.learningprojectserver.service;

import org.example.learningprojectserver.entities.AuditLogEntity;
import org.example.learningprojectserver.enums.AuditAction;
import org.example.learningprojectserver.mappers.AuditLogEntityToAuditLogDTOMapper;
import org.example.learningprojectserver.repository.AuditLogRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.AccessGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditServiceTest {

    private AuditLogRepository repository;
    private AccessGuard accessGuard;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        accessGuard = mock(AccessGuard.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        auditService = new AuditService(repository, new AuditLogEntityToAuditLogDTOMapper(), accessGuard, tx);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                Map.of("userId", "333", "username", "manager", "impersonatorId", "999"), null,
                List.of(new SimpleGrantedAuthority("ROLE_SCHOOLMANAGER"))));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordStoresActorImpersonatorAndValues() {
        auditService.record(AuditAction.STUDENT_CLASS_CHANGED, "USER", "111", "class=A1", "class=A2", "S1");

        ArgumentCaptor<AuditLogEntity> saved = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).save(saved.capture());
        AuditLogEntity entry = saved.getValue();
        assertEquals("333", entry.getActorUserId());
        assertEquals("SCHOOLMANAGER", entry.getActorRole());
        assertEquals("999", entry.getImpersonatorId());
        assertEquals("class=A1", entry.getBeforeValue());
        assertEquals("class=A2", entry.getAfterValue());
        assertEquals("S1", entry.getSchoolCode());
        assertNotNull(entry.getCreatedAt());
    }

    @Test
    void recordNeverThrowsWhenTheDatabaseFails() {
        when(repository.save(any())).thenThrow(new RuntimeException("db down"));
        assertDoesNotThrow(() ->
                auditService.record(AuditAction.LESSON_ADDED, "LESSON", "1", null, "x", "S1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void managerSearchIsLimitedToOwnSchool() {
        when(accessGuard.isAdmin()).thenReturn(false);
        when(accessGuard.currentSchoolCode()).thenReturn("S1");
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        BasicResponse<?> response = auditService.search(null, null, null, null, "S2", null, null, 0, 50);

        assertTrue(response.isSuccess());
        verify(accessGuard).currentSchoolCode();
    }

    @Test
    void invalidDateIsRejectedWithHebrewMessage() {
        when(accessGuard.isAdmin()).thenReturn(true);
        BasicResponse<?> response = auditService.search(null, null, null, null, null, "not-a-date", null, 0, 50);
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorCode().contains("תאריך"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void adminSearchReturnsMappedEntries() {
        when(accessGuard.isAdmin()).thenReturn(true);
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(1L);
        entity.setAction(AuditAction.IMPERSONATION_STARTED);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        BasicResponse<org.example.learningprojectserver.dto.AuditLogPageDTO> response =
                auditService.search("IMPERSONATION_STARTED", null, null, null, null, "2026-01-01", "2026-12-31", 0, 50);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().items().size());
        assertEquals("תחילת התחזות", response.getData().items().get(0).actionLabel());
    }
}
