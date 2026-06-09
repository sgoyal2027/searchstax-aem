package com.searchstax.aem.connector.core.listeners;

import com.day.cq.replication.Preprocessor;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.incremental.IndexingEventPaths;
import com.searchstax.aem.connector.core.incremental.IndexingScopeDecision;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.IncrementalIndexingQueueService;
import com.searchstax.aem.connector.core.services.IndexingScopeService;
import com.searchstax.aem.connector.core.services.SearchStaxDocumentBuilderService;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Preprocessor.class)
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

    @Reference
    private ResolverUtil resolverUtil;

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

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            for (final String path : IndexingEventPaths.dedupePaths(action.getPaths())) {
                if (indexingAction == IndexingAction.INDEX && !isInScope(resolver, path)) {
                    continue;
                }

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
        } catch (LoginException e) {
            LOG.error("{} Unable to evaluate replication scope; skipping indexing queue",
                    IncrementalIndexingDefaults.LOG_PREFIX, e);
        }
    }

    private boolean isInScope(final ResourceResolver resolver, final String path) {
        final IndexingScopeDecision decision = indexingScopeService.evaluate(resolver, path);
        if (!decision.isAccepted()) {
            LOG.debug("{} Replication preprocess skipped path={}: {}",
                    IncrementalIndexingDefaults.LOG_PREFIX,
                    path,
                    decision.getReason());
            return false;
        }
        return true;
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
