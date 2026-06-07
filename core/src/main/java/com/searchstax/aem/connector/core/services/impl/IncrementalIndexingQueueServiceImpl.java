package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.services.IncrementalIndexingQueueService;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component(service = IncrementalIndexingQueueService.class)
@Designate(ocd = IncrementalIndexingQueueServiceImpl.Config.class)
public class IncrementalIndexingQueueServiceImpl implements IncrementalIndexingQueueService {

    private static final Logger LOG = LoggerFactory.getLogger(IncrementalIndexingQueueServiceImpl.class);

    @Reference
    private JobManager jobManager;

    private final ConcurrentHashMap<String, IndexingAction> pendingActions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile ScheduledFuture<?> flushFuture;
    private volatile long debounceMs = IncrementalIndexingDefaults.DEBOUNCE_MS;

    @Activate
    protected void activate(final Config config) {
        debounceMs = config.debounceMs();
    }

    @Deactivate
    protected void deactivate() {
        if (flushFuture != null) {
            flushFuture.cancel(false);
        }
        scheduler.shutdownNow();
        pendingActions.clear();
    }

    @Override
    public synchronized void enqueue(final String path, final IndexingAction action) {
        if (path == null || path.isBlank() || action == null) {
            return;
        }

        final String normalizedPath = SearchStaxFullIndexPathConfigurationServiceImpl.normalizePath(path, false);
        if (normalizedPath.isEmpty()) {
            return;
        }

        pendingActions.put(normalizedPath, action);

        if (pendingActions.size() >= SearchStaxFullIndexDefaults.BATCH_SIZE) {
            LOG.info("{} Batch size limit reached ({}), flushing queue",
                    IncrementalIndexingDefaults.LOG_PREFIX, pendingActions.size());
            flushQueue();
            return;
        }

        scheduleFlush();
    }

    private void scheduleFlush() {
        if (flushFuture != null) {
            flushFuture.cancel(false);
        }
        flushFuture = scheduler.schedule(this::flushQueue, debounceMs, TimeUnit.MILLISECONDS);
    }

    private synchronized void flushQueue() {
        if (pendingActions.isEmpty()) {
            return;
        }

        final Map<String, IndexingAction> snapshot = new HashMap<>(pendingActions);
        pendingActions.clear();

        if (flushFuture != null) {
            flushFuture.cancel(false);
            flushFuture = null;
        }

        final List<String> indexPaths = new ArrayList<>();
        final List<String> deletePaths = new ArrayList<>();

        for (final Map.Entry<String, IndexingAction> entry : snapshot.entrySet()) {
            if (entry.getValue() == IndexingAction.INDEX) {
                indexPaths.add(entry.getKey());
            } else {
                deletePaths.add(entry.getKey());
            }
        }

        final String batchId = UUID.randomUUID().toString();
        final Map<String, Object> properties = new HashMap<>();
        properties.put(IncrementalIndexingDefaults.JOB_PROP_BATCH_ID, batchId);
        properties.put(IncrementalIndexingDefaults.JOB_PROP_INDEX_PATHS, indexPaths.toArray(new String[0]));
        properties.put(IncrementalIndexingDefaults.JOB_PROP_DELETE_PATHS, deletePaths.toArray(new String[0]));

        jobManager.addJob(IncrementalIndexingDefaults.JOB_TOPIC, properties);
        LOG.info("{} Flushed incremental batch {} with {} index and {} delete path(s)",
                IncrementalIndexingDefaults.LOG_PREFIX,
                batchId,
                indexPaths.size(),
                deletePaths.size());
    }

    @ObjectClassDefinition(name = "SearchStax Incremental Indexing Queue")
    public @interface Config {

        @AttributeDefinition(name = "Debounce window (ms)")
        long debounceMs() default IncrementalIndexingDefaults.DEBOUNCE_MS;
    }
}
