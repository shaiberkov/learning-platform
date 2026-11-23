package org.example.learningprojectserver.service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.learningprojectserver.dto.AuditLogDTO;
import org.example.learningprojectserver.dto.AuditLogPageDTO;
import org.example.learningprojectserver.entities.AuditLogEntity;
import org.example.learningprojectserver.enums.AuditAction;
import org.example.learningprojectserver.mappers.AuditLogEntityToAuditLogDTOMapper;
import org.example.learningprojectserver.repository.AuditLogRepository;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.security.AccessGuard;
import org.example.learningprojectserver.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Writes and reads the audit log. A write runs in its own transaction and never throws, so a
 * failure to audit cannot undo the user's action.
 */
@Slf4j
@Service
public class AuditService {

    private static final int MAX_PAGE_SIZE = 200;

    private final AuditLogRepository auditLogRepository;
    private final AuditLogEntityToAuditLogDTOMapper auditLogMapper;
    private final AccessGuard accessGuard;
    private final TransactionTemplate newTransaction;

    public AuditService(AuditLogRepository auditLogRepository,
                        AuditLogEntityToAuditLogDTOMapper auditLogMapper,
                        AccessGuard accessGuard,
                        PlatformTransactionManager transactionManager) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
        this.accessGuard = accessGuard;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void record(AuditAction action, String entityType, String entityId,
                       Object before, Object after, String schoolCode) {
        try {
            AuditLogEntity entry = new AuditLogEntity();
            entry.setCreatedAt(LocalDateTime.now());
            entry.setActorUserId(CurrentUser.userId());
            entry.setActorUsername(CurrentUser.username());
            entry.setActorRole(CurrentUser.role());
            entry.setImpersonatorId(CurrentUser.impersonatorId());
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setBeforeValue(truncate(before));
            entry.setAfterValue(truncate(after));
            entry.setIp(clientIp());
            entry.setSchoolCode(schoolCode);
            newTransaction.executeWithoutResult(status -> auditLogRepository.save(entry));
        } catch (RuntimeException e) {
            log.warn("Failed to write audit log entry {} for {} {}", action, entityType, entityId, e);
        }
    }

    /** System admins see every entry; a school manager sees only their own school's entries. */
    public BasicResponse<AuditLogPageDTO> search(String action, String actorUserId, String entityType,
                                                 String entityId, String schoolCode,
                                                 String from, String to, int page, int size) {
        String effectiveSchoolCode = schoolCode;
        if (!accessGuard.isAdmin()) {
            effectiveSchoolCode = accessGuard.currentSchoolCode();
            if (effectiveSchoolCode == null) {
                return new BasicResponse<>(false, "לא נמצא בית ספר המשויך למשתמש");
            }
        }

        AuditAction actionFilter = null;
        if (notBlank(action)) {
            try {
                actionFilter = AuditAction.valueOf(action);
            } catch (IllegalArgumentException e) {
                return new BasicResponse<>(false, "סוג פעולה לא מוכר");
            }
        }

        LocalDateTime fromTime;
        LocalDateTime toTime;
        try {
            fromTime = notBlank(from) ? LocalDate.parse(from).atStartOfDay() : null;
            toTime = notBlank(to) ? LocalDate.parse(to).plusDays(1).atStartOfDay() : null;
        } catch (DateTimeParseException e) {
            return new BasicResponse<>(false, "תאריך לא תקין, יש להזין בפורמט yyyy-MM-dd");
        }

        Specification<AuditLogEntity> spec = buildSpec(actionFilter, actorUserId, entityType, entityId,
                effectiveSchoolCode, fromTime, toTime);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Page<AuditLogEntity> result = auditLogRepository.findAll(spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt", "id")));

        List<AuditLogDTO> items = result.getContent().stream().map(auditLogMapper).toList();
        BasicResponse<AuditLogPageDTO> response = new BasicResponse<>(true, null);
        response.setData(new AuditLogPageDTO(items, result.getTotalElements(), safePage, safeSize));
        return response;
    }

    public BasicResponse<List<Map<String, String>>> actions() {
        BasicResponse<List<Map<String, String>>> response = new BasicResponse<>(true, null);
        response.setData(Arrays.stream(AuditAction.values())
                .map(a -> Map.of("value", a.name(), "label", a.getLabel()))
                .toList());
        return response;
    }

    public long countSince(LocalDateTime since) {
        return auditLogRepository.countByCreatedAtAfter(since);
    }

    private static Specification<AuditLogEntity> buildSpec(AuditAction action, String actorUserId, String entityType,
                                                           String entityId, String schoolCode,
                                                           LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (action != null) predicates.add(cb.equal(root.get("action"), action));
            if (notBlank(actorUserId)) predicates.add(cb.equal(root.get("actorUserId"), actorUserId.trim()));
            if (notBlank(entityType)) predicates.add(cb.equal(root.get("entityType"), entityType.trim()));
            if (notBlank(entityId)) predicates.add(cb.equal(root.get("entityId"), entityId.trim()));
            if (notBlank(schoolCode)) predicates.add(cb.equal(root.get("schoolCode"), schoolCode.trim()));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(cb.lessThan(root.get("createdAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String truncate(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.length() <= AuditLogEntity.VALUE_MAX_LENGTH
                ? text
                : text.substring(0, AuditLogEntity.VALUE_MAX_LENGTH);
    }

    private static String clientIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (notBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
