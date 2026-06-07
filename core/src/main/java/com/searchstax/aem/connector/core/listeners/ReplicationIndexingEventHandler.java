package com.searchstax.aem.connector.core.listeners;

import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.incremental.IndexingEventPaths;
import com.searchstax.aem.connector.core.services.IncrementalIndexingQueueService;
import com.searchstax.aem.connector.core.services.IndexingScopeService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = EventHandler.class,
        immediate = true,
        property = {
                EventConstants.EVENT_TOPIC + "=com/day/cq/replication/job",
                EventConstants.EVENT_TOPIC + "=com/day/cq/replication/action",
                EventConstants.EVENT_TOPIC + "=org/apache/sling/distribution/agent/package/distributed"
        }
)
public class ReplicationIndexingEventHandler implements EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ReplicationIndexingEventHandler.class);

    @Reference
    private IndexingScopeService indexingScopeService;

    @Reference
    private IncrementalIndexingQueueService incrementalIndexingQueueService;

    @Override
    public void handleEvent(final Event event) {
        if (!indexingScopeService.isConnectorEnabled()) {
            return;
        }

        final String type = IndexingEventPaths.readType(event);
        final IndexingAction action = IndexingEventPaths.mapEventType(type);
        if (action == null) {
            return;
        }

        final String[] paths = IndexingEventPaths.readPaths(event);
        if (paths.length == 0) {
            LOG.debug("{} Ignoring event topic={} with no paths (type={})",
                    IncrementalIndexingDefaults.LOG_PREFIX, event.getTopic(), type);
            return;
        }

        for (final String path : IndexingEventPaths.dedupePaths(paths)) {
            LOG.info("{} Distribution/replication event topic={} path={} type={} action={}",
                    IncrementalIndexingDefaults.LOG_PREFIX,
                    event.getTopic(),
                    path,
                    type,
                    action);

            incrementalIndexingQueueService.enqueue(path, action);
        }
    }
}
