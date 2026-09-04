package com.petmate.backend.report.dto;

import com.petmate.backend.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Signalement d'un utilisateur : la raison (obligatoire) et une description
 * libre (2 000 caractères max).
 */
public record ReportRequest(
        @NotNull(message = "reportedUserId ne peut pas être null")
        Long reportedUserId,

        @NotNull(message = "reason ne peut pas être null")
        ReportReason reason,

        @Size(max = 2000, message = "description ne peut pas dépasser 2000 caractères")
        String description) {
}