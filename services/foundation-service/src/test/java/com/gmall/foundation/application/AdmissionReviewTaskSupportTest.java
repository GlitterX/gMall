package com.gmall.foundation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gmall.foundation.domain.model.AdmissionApplicationStatus;
import com.gmall.foundation.domain.model.AdmissionReviewTaskStatus;
import com.gmall.foundation.infrastructure.persistence.AdmissionApplicationRepository;
import com.gmall.foundation.infrastructure.persistence.AdmissionReviewTaskEntity;
import com.gmall.foundation.infrastructure.persistence.AdmissionReviewTaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdmissionReviewTaskSupportTest {

    private final AdmissionReviewTaskRepository admissionReviewTaskRepository = mock(AdmissionReviewTaskRepository.class);
    private final AdmissionApplicationRepository admissionApplicationRepository = mock(AdmissionApplicationRepository.class);

    @Test
    void getWorkbenchOverviewAggregatesPendingActiveNearDueAndOverdueCounts() {
        AdmissionReviewTaskSupport support = new AdmissionReviewTaskSupport(
                admissionReviewTaskRepository,
                admissionApplicationRepository,
                Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneOffset.ofHours(8))
        );
        when(admissionApplicationRepository.countByApplicationStatus(AdmissionApplicationStatus.SUBMITTED)).thenReturn(4L);
        when(admissionReviewTaskRepository.findAllByTaskStatusOrderByLastOperatedAtDescTaskSequenceDesc(AdmissionReviewTaskStatus.ACTIVE))
                .thenReturn(List.of(
                        new AdmissionReviewTaskEntity(
                                "app-21",
                                1L,
                                AdmissionReviewTaskStatus.ACTIVE,
                                "reviewer-a",
                                OffsetDateTime.parse("2026-04-01T07:00:00+08:00"),
                                OffsetDateTime.parse("2026-04-01T11:00:00+08:00"),
                                "reviewer-a",
                                "开始审核",
                                OffsetDateTime.parse("2026-04-01T07:00:00+08:00"),
                                null,
                                1L
                        ),
                        new AdmissionReviewTaskEntity(
                                "app-22",
                                1L,
                                AdmissionReviewTaskStatus.ACTIVE,
                                "reviewer-b",
                                OffsetDateTime.parse("2026-04-01T06:00:00+08:00"),
                                OffsetDateTime.parse("2026-04-01T18:00:00+08:00"),
                                "reviewer-b",
                                "开始审核",
                                OffsetDateTime.parse("2026-04-01T06:00:00+08:00"),
                                null,
                                1L
                        ),
                        new AdmissionReviewTaskEntity(
                                "app-23",
                                1L,
                                AdmissionReviewTaskStatus.ACTIVE,
                                "reviewer-c",
                                OffsetDateTime.parse("2026-03-31T23:00:00+08:00"),
                                OffsetDateTime.parse("2026-04-01T07:30:00+08:00"),
                                "reviewer-c",
                                "开始审核",
                                OffsetDateTime.parse("2026-03-31T23:00:00+08:00"),
                                null,
                                1L
                        )
                ));

        FoundationQueryModels.AdmissionWorkbenchOverviewView actual = support.getWorkbenchOverview();

        assertThat(actual.pendingApplicationCount()).isEqualTo(4L);
        assertThat(actual.activeTaskCount()).isEqualTo(3L);
        assertThat(actual.nearDueTaskCount()).isEqualTo(1L);
        assertThat(actual.overdueTaskCount()).isEqualTo(1L);
    }
}
