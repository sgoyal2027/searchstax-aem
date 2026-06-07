package com.searchstax.aem.connector.core.listeners;

import com.day.cq.replication.Preprocessor;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.incremental.IndexingEventPaths;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.IncrementalIndexingQueueService;
import com.searchstax.aem.connector.core.services.IndexingScopeService;
import com.searchstax.aem.connector.core.services.SearchStaxDocumentBuilderService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Preprocessor.class, immediate = true)
public class SearchStaxReplicationPreprocessor implements Preprocessor {

    private static final Logger LOG = LoggerFactory.getLogger(SearchStaxReplicationPreprocessor.class);

    @Reference
    private IndexingScopeService indexingScopeService;

    @Reference
    private IncrementalIndexingQueueService incrementalIndexingQueueService;

    @Reference
    private IndexingAuditService indexingAuditService;

    @Reference
    private SearchStaxDocumentBuilderService documentBuilderService;

    @Override
    public void preprocess(final ReplicationAction action, final ReplicationOptions options)
            throws ReplicationException {

        if (action == null || !indexingScopeService.isConnectorEnabled()) {
            return;
        }

        final IndexingAction indexingAction = mapReplicationType(action.getType());
        if (indexingAction == null) {
            return;
        }

        for (final String path : IndexingEventPaths.dedupePaths(action.getPaths())) {
            LOG.info("{} Replication preprocess path={} type={} action={}",
                    IncrementalIndexingDefaults.LOG_PREFIX,
                    path,
                    action.getType(),
                    indexingAction);

            incrementalIndexingQueueService.enqueue(path, indexingAction);
            indexingAuditService.record(
                    path,
                    indexingAction,
                    "QUEUED",
                    "replication",
                    0,
                    "Queued after replication " + action.getType(),
                    0L,
                    documentBuilderService.resolveDocumentId(path));
        }
    }

    private IndexingAction mapReplicationType(final ReplicationActionType type) {
        if (type == null) {
            return null;
        }

        switch (type) {
            case ACTIVATE:
                return IndexingAction.INDEX;
            case DEACTIVATE:
            case DELETE:
                return IndexingAction.DELETE;
            default:
                return null;
        }
    }
}
