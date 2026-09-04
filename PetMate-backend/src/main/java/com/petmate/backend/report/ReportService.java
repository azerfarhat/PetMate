package com.petmate.backend.report;

import com.petmate.backend.entity.Report;
import com.petmate.backend.entity.User;
import com.petmate.backend.enums.ReportStatus;
import com.petmate.backend.exception.ReportException;
import com.petmate.backend.exception.UserNotFoundException;
import com.petmate.backend.repository.ReportRepository;
import com.petmate.backend.repository.UserRepository;
import com.petmate.backend.report.dto.ReportRequest;
import com.petmate.backend.report.dto.ReportResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Signalement d'un utilisateur (modération). Un signalement est créé en statut
 * {@code PENDING} et reste consultable par son auteur via son historique.
 *
 * <p>Règles : on ne peut pas se signaler soi-même, la cible doit exister et être
 * un compte actif, et un signalement non clôturé entre les deux mêmes
 * utilisateurs bloque un nouveau doublon (anti-spam).</p>
 */
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public ReportService(ReportRepository reportRepository, UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    /**
     * Crée un signalement en statut {@code PENDING}.
     *
     * @throws ReportException     si on se signale soi-même ou si un signalement
     *                             non clôturé existe déjà entre les deux utilisateurs
     * @throws UserNotFoundException si l'utilisateur signalé n'existe pas ou est inactif
     */
    @Transactional
    public ReportResponse report(Long reporterId, ReportRequest request) {
        Long reportedUserId = request.reportedUserId();
        if (reporterId.equals(reportedUserId)) {
            throw new ReportException("Impossible de se signaler soi-même");
        }

        User reported = userRepository.findById(reportedUserId)
                .filter(User::isActive)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        if (reportRepository.hasOpenReportBetween(reporterId, reportedUserId,
                List.of(ReportStatus.RESOLVED, ReportStatus.REJECTED))) {
            throw new ReportException("Vous avez déjà signalé cet utilisateur. Le signalement précédent est toujours en cours de traitement.");
        }

        Report report = reportRepository.save(Report.builder()
                .reporter(User.builder().id(reporterId).build())
                .reportedUser(reported)
                .reason(request.reason())
                .description(blankToNull(request.description()))
                .status(ReportStatus.PENDING)
                .build());

        return toResponse(report);
    }

    /**
     * Historique des signalements émis par {@code reporterId}, plus récents d'abord.
     */
    @Transactional(readOnly = true)
    public List<ReportResponse> myReports(Long reporterId) {
        return reportRepository.findReportsByReporterId(reporterId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ReportResponse toResponse(Report report) {
        User reported = report.getReportedUser();
        return new ReportResponse(
                report.getId(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                reported.getId(),
                reported.getFirstName(),
                reported.getLastName(),
                report.getCreatedAt(),
                report.getResolvedAt());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}