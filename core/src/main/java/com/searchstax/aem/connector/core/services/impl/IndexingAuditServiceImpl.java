package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.incremental.IndexingAuditRecord;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.query.Query;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Component(service = IndexingAuditService.class)
public class IndexingAuditServiceImpl implements IndexingAuditService {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingAuditServiceImpl.class);

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Reference
    private ResolverUtil resolverUtil;

    @Override
    public void record(
            final String path,
            final IndexingAction action,
            final String status,
            final String batchId,
            final int httpStatus,
            final String message,
            final long durationMs,
            final String documentId) {

        final long timestampMs = Instant.now().toEpochMilli();
        final String timestamp = ISO_FORMATTER.format(Instant.ofEpochMilli(timestampMs));
        final String normalizedStatus = status == null || status.isBlank() ? "FAILURE" : status;

        LOG.info("{} Audit path={} action={} status={} batchId={} httpStatus={} durationMs={} message={}",
                IncrementalIndexingDefaults.LOG_PREFIX,
                path,
                action,
                normalizedStatus,
                batchId,
                httpStatus,
                durationMs,
                message);

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource auditRoot = ensureAuditRoot(resolver);
            if (auditRoot == null) {
                LOG.error("{} Unable to create audit root {}", IncrementalIndexingDefaults.LOG_PREFIX,
                        IncrementalIndexingDefaults.AUDIT_ROOT);
                return;
            }

            final String nodeName = "entry-" + timestampMs + "-" + UUID.randomUUID();
            final Resource entry = ResourceUtil.getOrCreateResource(
                    resolver,
                    auditRoot.getPath() + "/" + nodeName,
                    "nt:unstructured",
                    "nt:unstructured",
                    false);

            final ModifiableValueMap properties = entry.adaptTo(ModifiableValueMap.class);
            if (properties == null) {
                return;
            }

            properties.put("timestamp", timestamp);
            properties.put("timestampMs", timestampMs);
            properties.put("path", path);
            properties.put("action", action.name());
            properties.put("status", normalizedStatus);
            properties.put("batchId", batchId);
            properties.put("httpStatus", httpStatus);
            properties.put("message", message == null ? "" : message);
            properties.put("durationMs", durationMs);
            properties.put("documentId", documentId == null ? "" : documentId);

            resolver.commit();
        } catch (Exception e) {
            LOG.error("{} Failed to persist indexing audit record for {}", IncrementalIndexingDefaults.LOG_PREFIX,
                    path, e);
        }
    }

    @Override
    public List<IndexingAuditRecord> getRecordsForLast24Hours() {
        final long cutoffMs = Instant.now().toEpochMilli() - IncrementalIndexingDefaults.AUDIT_RETENTION_MS;

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final List<IndexingAuditRecord> records = queryByTimestampMs(resolver, cutoffMs);
            if (!records.isEmpty()) {
                return records;
            }
            return listChildrenSince(resolver, cutoffMs);
        } catch (Exception e) {
            LOG.error("{} Failed to load indexing audit records", IncrementalIndexingDefaults.LOG_PREFIX, e);
            return Collections.emptyList();
        }
    }

    @Override
    public int purgeOlderThanRetention() {
        final long cutoffMs = Instant.now().toEpochMilli() - IncrementalIndexingDefaults.AUDIT_RETENTION_MS;
        int removed = 0;

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource auditRoot = resolver.getResource(IncrementalIndexingDefaults.AUDIT_ROOT);
            if (auditRoot == null) {
                return 0;
            }

            for (final Resource child : auditRoot.getChildren()) {
                final long timestampMs = child.getValueMap().get("timestampMs", 0L);
                if (timestampMs > 0L && timestampMs < cutoffMs) {
                    resolver.delete(child);
                    removed++;
                }
            }

            if (removed > 0) {
                resolver.commit();
                LOG.info("{} Purged {} indexing audit record(s) older than 24 hours",
                        IncrementalIndexingDefaults.LOG_PREFIX, removed);
            }
        } catch (PersistenceException e) {
            LOG.error("{} Failed to purge old indexing audit records", IncrementalIndexingDefaults.LOG_PREFIX, e);
        } catch (Exception e) {
            LOG.error("{} Failed to query old indexing audit records", IncrementalIndexingDefaults.LOG_PREFIX, e);
        }

        return removed;
    }

    private List<IndexingAuditRecord> queryByTimestampMs(final ResourceResolver resolver, final long cutoffMs) {
        final String sql2 = "SELECT * FROM [nt:unstructured] AS node "
                + "WHERE ISDESCENDANTNODE(node, '" + IncrementalIndexingDefaults.AUDIT_ROOT + "') "
                + "AND node.[timestampMs] >= " + cutoffMs + " "
                + "ORDER BY node.[timestampMs] DESC";

        final Iterator<Resource> resources = resolver.findResources(sql2, Query.JCR_SQL2);
        final List<IndexingAuditRecord> records = new ArrayList<>();

        while (resources.hasNext()) {
            records.add(toRecord(resources.next()));
        }

        return records;
    }

    private List<IndexingAuditRecord> listChildrenSince(final ResourceResolver resolver, final long cutoffMs) {
        final Resource auditRoot = resolver.getResource(IncrementalIndexingDefaults.AUDIT_ROOT);
        if (auditRoot == null) {
            return Collections.emptyList();
        }

        final List<IndexingAuditRecord> records = new ArrayList<>();
        for (final Resource child : auditRoot.getChildren()) {
            final long timestampMs = child.getValueMap().get("timestampMs", 0L);
            if (timestampMs == 0L) {
                final String timestamp = child.getValueMap().get("timestamp", String.class);
                if (timestamp != null) {
                    try {
                        if (Instant.parse(timestamp).toEpochMilli() >= cutoffMs) {
                            records.add(toRecord(child));
                        }
                    } catch (Exception ignored) {
                        // skip malformed entries
                    }
                }
                continue;
            }

            if (timestampMs >= cutoffMs) {
                records.add(toRecord(child));
            }
        }

        records.sort(Comparator.<IndexingAuditRecord>comparingLong(
                record -> parseTimestampMs(record.getTimestamp())).reversed());
        return records;
    }

    private long parseTimestampMs(final String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return 0L;
        }
        try {
            return Instant.parse(timestamp).toEpochMilli();
        } catch (Exception e) {
            return 0L;
        }
    }

    private Resource ensureAuditRoot(final ResourceResolver resolver) throws PersistenceException {
        return ResourceUtil.getOrCreateResource(
                resolver,
                IncrementalIndexingDefaults.AUDIT_ROOT,
                "sling:Folder",
                "sling:Folder",
                true);
    }

    private IndexingAuditRecord toRecord(final Resource resource) {
        return new IndexingAuditRecord(
                resource.getValueMap().get("timestamp", String.class),
                resource.getValueMap().get("path", String.class),
                IndexingAction.valueOf(resource.getValueMap().get("action", IndexingAction.INDEX.name())),
                resource.getValueMap().get("status", String.class),
                resource.getValueMap().get("batchId", String.class),
                resource.getValueMap().get("httpStatus", 0),
                resource.getValueMap().get("message", String.class),
                resource.getValueMap().get("durationMs", 0L),
                resource.getValueMap().get("documentId", String.class));
    }
}
