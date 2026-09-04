package com.petmate.backend.report;

import com.petmate.backend.entity.Report;
import com.petmate.backend.entity.User;
import com.petmate.backend.enums.ReportReason;
import com.petmate.backend.enums.ReportStatus;
import com.petmate.backend.exception.ReportException;
import com.petmate.backend.exception.UserNotFoundException;
import com.petmate.backend.report.dto.ReportRequest;
import com.petmate.backend.report.dto.ReportResponse;
import com.petmate.backend.repository.ReportRepository;
import com.petmate.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un signalement est personnel : on signale en son nom, un signalement ouvre en
 * statut {@code PENDING}, reste consultable dans son historique, et un signalement
 * encore ouvert (ni {@code RESOLVED} ni {@code REJECTED}) bloque le doublon.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final long ME = 1L;
    private static final long OTHER = 2L;

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, userRepository);
    }

    @Test
    void report_createsPendingReport() {
        when(userRepository.findById(OTHER)).thenReturn(Optional.of(user(OTHER)));
        when(reportRepository.hasOpenReportBetween(ME, OTHER, List.of(ReportStatus.RESOLVED, ReportStatus.REJECTED)))
                .thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportResponse response = reportService.report(ME, request(ReportReason.SCAM, "Arnaque"));

        assertEquals(ReportReason.SCAM, response.reason());
        assertEquals(ReportStatus.PENDING, response.status());
        assertEquals(OTHER, response.reportedUserId());
        assertEquals("Arnaque", response.description());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void report_blankDescription_isStoredAsNull() {
        when(userRepository.findById(OTHER)).thenReturn(Optional.of(user(OTHER)));
        when(reportRepository.hasOpenReportBetween(ME, OTHER, List.of(ReportStatus.RESOLVED, ReportStatus.REJECTED)))
                .thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportResponse response = reportService.report(ME, request(ReportReason.OTHER, "   "));

        assertNull(response.description());
    }

    @Test
    void report_self_throwsReportException() {
        assertThrows(ReportException.class,
                () -> reportService.report(ME, new ReportRequest(ME, ReportReason.FAKE_PROFILE, null)));
    }

    @Test
    void report_unknownUser_throwsNotFound() {
        when(userRepository.findById(OTHER)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> reportService.report(ME, request(ReportReason.FAKE_PROFILE, null)));
    }

    @Test
    void report_inactiveUser_throwsNotFound() {
        User inactive = user(OTHER);
        inactive.setActive(false);
        when(userRepository.findById(OTHER)).thenReturn(Optional.of(inactive));

        assertThrows(UserNotFoundException.class,
                () -> reportService.report(ME, request(ReportReason.FAKE_PROFILE, null)));
    }

    @Test
    void report_openReportAlreadyExists_throwsReportException() {
        when(userRepository.findById(OTHER)).thenReturn(Optional.of(user(OTHER)));
        when(reportRepository.hasOpenReportBetween(eq(ME), eq(OTHER), any()))
                .thenReturn(true);

        assertThrows(ReportException.class,
                () -> reportService.report(ME, request(ReportReason.HARASSMENT, "Harcèlement")));
    }

    @Test
    void report_afterClosedReport_allowsNewOne() {
        when(userRepository.findById(OTHER)).thenReturn(Optional.of(user(OTHER)));
        when(reportRepository.hasOpenReportBetween(eq(ME), eq(OTHER), any())).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportResponse response = reportService.report(ME, request(ReportReason.SCAM, "Tentative d'arnaque"));

        assertEquals(ReportStatus.PENDING, response.status());
    }

    @Test
    void myReports_returnsOwnReportsNewestFirst() {
        when(reportRepository.findReportsByReporterId(ME))
                .thenReturn(List.of(report(10L, ReportReason.SCAM), report(11L, ReportReason.ANIMAL_ABUSE)));

        List<ReportResponse> response = reportService.myReports(ME);

        assertEquals(2, response.size());
        assertEquals(10L, response.get(0).id());
        assertEquals(ReportReason.ANIMAL_ABUSE, response.get(1).reason());
    }

    private ReportRequest request(ReportReason reason, String description) {
        return new ReportRequest(OTHER, reason, description);
    }

    private Report report(Long id, ReportReason reason) {
        return Report.builder()
                .id(id)
                .reporter(User.builder().id(ME).build())
                .reportedUser(user(OTHER))
                .reason(reason)
                .description("description")
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .build();
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .firstName("Emma")
                .lastName("Test")
                .active(true)
                .build();
    }
}