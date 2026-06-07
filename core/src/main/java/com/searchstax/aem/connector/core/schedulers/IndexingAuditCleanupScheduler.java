package com.searchstax.aem.connector.core.schedulers;

import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class IndexingAuditCleanupScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingAuditCleanupScheduler.class);

    private static final String JOB_NAME = "searchstax-indexing-audit-cleanup";

    @Reference
    private Scheduler scheduler;

    @Reference
    private IndexingAuditService indexingAuditService;

    @Activate
    protected void activate() {
        final ScheduleOptions options = scheduler.EXPR("0 0 * * * ?");
        options.name(JOB_NAME);
        options.canRunConcurrently(false);
        scheduler.schedule(this, options);
        LOG.info("{} Scheduled indexing audit cleanup job", IncrementalIndexingDefaults.LOG_PREFIX);
    }

    @Deactivate
    protected void deactivate() {
        scheduler.unschedule(JOB_NAME);
    }

    @Override
    public void run() {
        final int removed = indexingAuditService.purgeOlderThanRetention();
        LOG.info("{} Audit cleanup completed, removed {} record(s)", IncrementalIndexingDefaults.LOG_PREFIX, removed);
    }
}
