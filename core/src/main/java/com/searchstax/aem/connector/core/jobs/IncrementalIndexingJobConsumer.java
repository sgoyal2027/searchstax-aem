package com.searchstax.aem.connector.core.jobs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.dto.response.IndexingBatchResult;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.services.IndexingApiService;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.IndexingFailureNotificationService;
import com.searchstax.aem.connector.core.services.IndexingScopeService;
import com.searchstax.aem.connector.core.services.SearchStaxDocumentBuilderService;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component(
        service = JobConsumer.class,
        property = {
                JobConsumer.PROPERTY_TOPICS + "=" + IncrementalIndexingDefaults.JOB_TOPIC
        }
)
public class IncrementalIndexingJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(IncrementalIndexingJobConsumer.class);

    @Reference
    private ResolverUtil resolverUtil;

    @Reference
    private IndexingScopeService indexingScopeService;

    @Reference
    private SearchStaxDocumentBuilderService documentBuilderService;

    @Reference
    private IndexingApiService indexingApiService;

    @Reference
    private IndexingAuditService indexingAuditService;

    @Reference
    private IndexingFailureNotificationService failureNotificationService;

    @Override
    public JobResult process(final Job job) {
        if (!indexingScopeService.isConnectorEnabled()) {
            LOG.info("{} Skipping incremental job: connector disabled", IncrementalIndexingDefaults.LOG_PREFIX);
            return JobResult.OK;
        }

        final String batchId = job.getProperty(IncrementalIndexingDefaults.JOB_PROP_BATCH_ID, String.class);
        final String[] indexPaths = job.getProperty(IncrementalIndexingDefaults.JOB_PROP_INDEX_PATHS, String[].class);
        final String[] deletePaths = job.getProperty(IncrementalIndexingDefaults.JOB_PROP_DELETE_PATHS, String[].class);

        LOG.info("{} Processing incremental job batchId={} indexCount={} deleteCount={}",
                IncrementalIndexingDefaults.LOG_PREFIX,
                batchId,
                indexPaths == null ? 0 : indexPaths.length,
                deletePaths == null ? 0 : deletePaths.length);

        processDeletes(batchId, deletePaths);
        processIndexes(batchId, indexPaths);

        indexingAuditService.purgeOlderThanRetention();
        return JobResult.OK;
    }

    private void processIndexes(final String batchId, final String[] indexPaths) {
        if (indexPaths == null || indexPaths.length == 0) {
            return;
        }

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final List<String> documentBodies = new ArrayList<>();
            final List<String> indexedPaths = new ArrayList<>();

            for (final String path : indexPaths) {
                if (!isInScope(resolver, path, IndexingAction.INDEX)) {
                    continue;
                }

                final Optional<ObjectNode> document = documentBuilderService.buildDocument(resolver, path);
                if (document.isPresent()) {
                    documentBodies.add(document.get().toString());
                    indexedPaths.add(path);
                } else {
                    indexingAuditService.record(
                            path,
                            IndexingAction.INDEX,
                            "FAILURE",
                            batchId,
                            0,
                            "Unable to build document (missing resource, mapping error, or exceeds 100 KB limit)",
                            0L,
                            documentBuilderService.resolveDocumentId(path));
                }

                if (documentBodies.size() >= SearchStaxFullIndexDefaults.BATCH_SIZE) {
                    flushIndexBatch(batchId, documentBodies, indexedPaths);
                    documentBodies.clear();
                    indexedPaths.clear();
                }
            }

            if (!documentBodies.isEmpty()) {
                flushIndexBatch(batchId, documentBodies, indexedPaths);
            }
        } catch (Exception e) {
            LOG.error("{} Failed to process index paths for batch {}", IncrementalIndexingDefaults.LOG_PREFIX,
                    batchId, e);
        }
    }

    private void flushIndexBatch(
            final String batchId,
            final List<String> documentBodies,
            final List<String> indexedPaths) {

        final IndexingBatchResult result = indexingApiService.indexDocuments(documentBodies);
        final long perItemDuration = indexedPaths.isEmpty() ? 0L : result.getDurationMs() / indexedPaths.size();

        for (final String path : indexedPaths) {
            indexingAuditService.record(
                    path,
                    IndexingAction.INDEX,
                    result.isSuccess() ? "SUCCESS" : "FAILURE",
                    batchId,
                    result.getStatusCode(),
                    result.getMessage(),
                    perItemDuration,
                    documentBuilderService.resolveDocumentId(path));
        }

        if (!result.isSuccess()) {
            failureNotificationService.notifyBatchFailure(batchId, IndexingAction.INDEX, result, indexedPaths);
        }
    }

    private void processDeletes(final String batchId, final String[] deletePaths) {
        if (deletePaths == null || deletePaths.length == 0) {
            return;
        }

        final List<String> documentIds = new ArrayList<>();
        final List<String> deletedPaths = new ArrayList<>();

        for (final String path : deletePaths) {
            final String documentId = documentBuilderService.resolveDocumentId(path);
            if (!documentId.isEmpty()) {
                documentIds.add(documentId);
                deletedPaths.add(path);
            }

            if (documentIds.size() >= SearchStaxFullIndexDefaults.BATCH_SIZE) {
                flushDeleteBatch(batchId, documentIds, deletedPaths);
                documentIds.clear();
                deletedPaths.clear();
            }
        }

        if (!documentIds.isEmpty()) {
            flushDeleteBatch(batchId, documentIds, deletedPaths);
        }
    }

    private void flushDeleteBatch(
            final String batchId,
            final List<String> documentIds,
            final List<String> deletedPaths) {

        final IndexingBatchResult result = indexingApiService.deleteDocuments(documentIds);
        final long perItemDuration = deletedPaths.isEmpty() ? 0L : result.getDurationMs() / deletedPaths.size();

        for (final String path : deletedPaths) {
            indexingAuditService.record(
                    path,
                    IndexingAction.DELETE,
                    result.isSuccess() ? "SUCCESS" : "FAILURE",
                    batchId,
                    result.getStatusCode(),
                    result.getMessage(),
                    perItemDuration,
                    documentBuilderService.resolveDocumentId(path));
        }

        if (!result.isSuccess()) {
            failureNotificationService.notifyBatchFailure(batchId, IndexingAction.DELETE, result, deletedPaths);
        }
    }

    private boolean isInScope(
            final ResourceResolver resolver,
            final String path,
            final IndexingAction action) {

        if (action == IndexingAction.DELETE) {
            return true;
        }

        final com.searchstax.aem.connector.core.incremental.IndexingScopeDecision decision =
                indexingScopeService.evaluate(resolver, path);
        if (!decision.isAccepted()) {
            LOG.info("{} Skipping path {}: {}", IncrementalIndexingDefaults.LOG_PREFIX, path, decision.getReason());
            indexingAuditService.record(
                    path,
                    action,
                    "SKIPPED",
                    "scope",
                    0,
                    decision.getReason(),
                    0L,
                    documentBuilderService.resolveDocumentId(path));
            return false;
        }
        return true;
    }
}
