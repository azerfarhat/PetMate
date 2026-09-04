package com.petmate.backend.repository;

import com.petmate.backend.entity.Report;
import com.petmate.backend.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Vrai si un signalement encore ouvert (statut différent de {@code RESOLVED}
     * et {@code REJECTED}) existe déjà entre ce reporter et cet utilisateur :
     * évite le spam de signalements. Les signalements clôturés n'empêchent pas
     * un nouveau signalement.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Report r
            WHERE r.reporter.id = :reporterId
              AND r.reportedUser.id = :reportedUserId
              AND r.status NOT IN :closedStatuses
            """)
    boolean hasOpenReportBetween(@Param("reporterId") Long reporterId,
                                 @Param("reportedUserId") Long reportedUserId,
                                 @Param("closedStatuses") Collection<ReportStatus> closedStatuses);

    /**
     * Signalements émis par {@code reporterId}, les plus récents d'abord. Le
     * reporter et l'utilisateur signalé sont fetch-és avec le signalement
     * (aucune collection List fetch-ée, donc aucun MultipleBagFetchException).
     */
    @Query("""
            SELECT r FROM Report r
            JOIN FETCH r.reporter
            JOIN FETCH r.reportedUser
            WHERE r.reporter.id = :reporterId
            ORDER BY r.createdAt DESC
            """)
    List<Report> findReportsByReporterId(@Param("reporterId") Long reporterId);
}