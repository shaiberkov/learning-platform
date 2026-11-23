package org.example.learningprojectserver.dto;

import java.util.List;

public record AuditLogPageDTO(List<AuditLogDTO> items, long total, int page, int size) {}
