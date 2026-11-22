package org.example.learningprojectserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.dto.AuditLogPageDTO;
import org.example.learningprojectserver.response.BasicResponse;
import org.example.learningprojectserver.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.example.learningprojectserver.constants.ControllerConstants.AuditLog.*;

/** System admins and school managers (own school only, enforced in AuditService). */
@RestController
@RequestMapping(AUDIT_LOG_BASE_PATH)
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping(LIST_AUDIT_LOG)
    public BasicResponse<AuditLogPageDTO> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorUserId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String schoolCode,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return auditService.search(action, actorUserId, entityType, entityId, schoolCode, from, to, page, size);
    }

    @GetMapping(AUDIT_ACTIONS)
    public BasicResponse<List<Map<String, String>>> actions() {
        return auditService.actions();
    }
}
