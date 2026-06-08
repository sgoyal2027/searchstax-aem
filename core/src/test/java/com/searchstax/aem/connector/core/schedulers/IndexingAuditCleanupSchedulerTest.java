package com.searchstax.aem.connector.core.schedulers;

import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IndexingAuditCleanupSchedulerTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private ScheduleOptions scheduleOptions;

    @Mock
    private IndexingAuditService indexingAuditService;

    private IndexingAuditCleanupScheduler cleanupScheduler;

    @BeforeEach
    void setUp() {
        when(scheduler.EXPR(anyString())).thenReturn(scheduleOptions);
        when(scheduleOptions.name(anyString())).thenReturn(scheduleOptions);
        when(scheduleOptions.canRunConcurrently(false)).thenReturn(scheduleOptions);

        cleanupScheduler = new IndexingAuditCleanupScheduler();
        TestReflection.inject(cleanupScheduler, "scheduler", scheduler);
        TestReflection.inject(cleanupScheduler, "indexingAuditService", indexingAuditService);
    }

    @Test
    void activateSchedulesHourlyCleanupJob() {
        cleanupScheduler.activate();

        verify(scheduler).schedule(cleanupScheduler, scheduleOptions);
    }

    @Test
    void deactivateUnschedulesCleanupJob() {
        cleanupScheduler.deactivate();

        verify(scheduler).unschedule("searchstax-indexing-audit-cleanup");
    }

    @Test
    void runPurgesOldAuditRecords() {
        when(indexingAuditService.purgeOlderThanRetention()).thenReturn(3);

        cleanupScheduler.run();

        verify(indexingAuditService).purgeOlderThanRetention();
    }
}
