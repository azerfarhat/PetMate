package com.petmate.backend.report;

import com.petmate.backend.report.dto.ReportRequest;
import com.petmate.backend.report.dto.ReportResponse;
import com.petmate.backend.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * API REST des signalements de modération (protégée par JWT). L'identifiant est
 * toujours pris du principal authentifié : on signale en son nom et on ne voit
 * que son propre historique (pas d'IDOR).
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Signale un utilisateur. Le signalement est créé en statut {@code PENDING}.
     */
    @PostMapping
    public ResponseEntity<ReportResponse> report(
            Authentication authentication,
            @Valid @RequestBody ReportRequest request) {
        ReportResponse response = reportService.report(userId(authentication), request);
        return ResponseEntity.created(URI.create("/reports/" + response.id())).body(response);
    }

    /**
     * Historique de mes signalements, les plus récents d'abord.
     */
    @GetMapping("/my")
    public ResponseEntity<List<ReportResponse>> myReports(Authentication authentication) {
        return ResponseEntity.ok(reportService.myReports(userId(authentication)));
    }

    private long userId(Authentication authentication) {
        return ((AuthUserPrincipal) authentication.getPrincipal()).getUserId();
    }
}