package com.petmate.backend.report.dto;

import com.petmate.backend.enums.ReportReason;
import com.petmate.backend.enums.ReportStatus;

import java.time.LocalDateTime;

/**
 * Signalement émis : identifiant, raison, description, statut de modération et
 * informations minimales de l'utilisateur signalé.
 */
public record ReportResponse(
        Long id,
        ReportReason reason,
        String description,
        ReportStatus status,
        Long reportedUserId,
        String reportedFirstName,
        String reportedLastName,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt) {
}