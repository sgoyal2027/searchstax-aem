package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.incremental.IndexingAuditRecord;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import javax.jcr.query.Query;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class IndexingAuditServiceImplTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    private IndexingAuditService auditService;

    @BeforeEach
    void setUp() throws Exception {
        context.create().resource(IncrementalIndexingDefaults.AUDIT_ROOT, "sling:resourceType", "sling:Folder");
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());

        auditService = new IndexingAuditServiceImpl();
        context.registerService(ResolverUtil.class, resolverUtil);
        context.registerInjectActivateService(auditService);
    }

    @Test
    void recordsAndLoadsAuditEntries() {
        auditService.record(
                "/content/wknd/us/en/page",
                IndexingAction.INDEX,
                "SUCCESS",
                "batch-1",
                200,
                "Indexed documents",
                15L,
                "/content/wknd/us/en/page");

        final List<IndexingAuditRecord> records = auditService.getRecordsForLast24Hours();

        assertFalse(records.isEmpty());
        final IndexingAuditRecord record = records.get(0);
        assertEquals("/content/wknd/us/en/page", record.getPath());
        assertEquals("SUCCESS", record.getStatus());
        assertEquals("batch-1", record.getBatchId());
    }

    @Test
    void purgesEntriesOlderThanRetentionWindow() {
        final long oldTimestamp = Instant.now().toEpochMilli() - IncrementalIndexingDefaults.AUDIT_RETENTION_MS - 60_000L;
        context.create().resource(
                IncrementalIndexingDefaults.AUDIT_ROOT + "/entry-old",
                "timestampMs",
                oldTimestamp,
                "timestamp",
                Instant.ofEpochMilli(oldTimestamp).toString(),
                "path",
                "/content/old",
                "action",
                IndexingAction.INDEX.name(),
                "status",
                "SUCCESS");

        auditService.record(
                "/content/new",
                IndexingAction.INDEX,
                "SUCCESS",
                "batch-2",
                200,
                "ok",
                1L,
                "/content/new");

        final int removed = auditService.purgeOlderThanRetention();
        final List<IndexingAuditRecord> records = auditService.getRecordsForLast24Hours();

        assertEquals(1, removed);
        assertEquals(1, records.size());
        assertEquals("/content/new", records.get(0).getPath());
    }

    @Test
    void defaultsMissingStatusToFailure() {
        auditService.record(
                "/content/wknd/page",
                IndexingAction.DELETE,
                " ",
                "batch-3",
                500,
                "failed",
                2L,
                "/content/wknd/page");

        final List<IndexingAuditRecord> records = auditService.getRecordsForLast24Hours();

        assertTrue(records.stream().anyMatch(record -> "FAILURE".equals(record.getStatus())));
    }

    @Test
    void loadsRecordsUsingTimestampOnlyFallback() {
        final long recentTimestamp = Instant.now().toEpochMilli();
        context.create().resource(
                IncrementalIndexingDefaults.AUDIT_ROOT + "/entry-iso",
                "timestamp",
                Instant.ofEpochMilli(recentTimestamp).toString(),
                "path",
                "/content/iso",
                "action",
                IndexingAction.INDEX.name(),
                "status",
                "SUCCESS");

        final List<IndexingAuditRecord> records = auditService.getRecordsForLast24Hours();

        assertTrue(records.stream().anyMatch(record -> "/content/iso".equals(record.getPath())));
    }

    @Test
    void returnsEmptyListWhenResolverLoginFails() throws Exception {
        when(resolverUtil.getServiceResolver()).thenThrow(new LoginException("login failed"));

        assertTrue(auditService.getRecordsForLast24Hours().isEmpty());
    }

    @Test
    void purgeReturnsZeroWhenAuditRootMissing() throws Exception {
        context.resourceResolver().delete(context.resourceResolver().getResource(IncrementalIndexingDefaults.AUDIT_ROOT));

        assertEquals(0, auditService.purgeOlderThanRetention());
    }

    @Test
    void swallowsPersistenceErrorsDuringRecord() throws Exception {
        when(resolverUtil.getServiceResolver()).thenThrow(new LoginException("login failed"));

        auditService.record(
                "/content/wknd/page",
                IndexingAction.INDEX,
                "SUCCESS",
                "batch-4",
                200,
                "ok",
                1L,
                "/content/wknd/page");
    }

    @Test
    void loadsRecordsUsingSql2QueryWhenResourcesReturned() throws Exception {
        final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);

        final Resource resource = Mockito.mock(Resource.class);
        when(resource.getValueMap()).thenReturn(
                new ValueMapDecorator(Map.of(
                        "timestamp", Instant.now().toString(),
                        "path", "/content/wknd",
                        "action", IndexingAction.INDEX.name(),
                        "status", "SUCCESS",
                        "batchId", "batch-1",
                        "httpStatus", 200,
                        "message", "ok",
                        "durationMs", 123L,
                        "documentId", "doc-1")));

        when(resolver.findResources(anyString(), eq(Query.JCR_SQL2)))
                .thenReturn(Collections.singletonList(resource).iterator());

        final List<IndexingAuditRecord> records = auditService.getRecordsForLast24Hours();

        assertEquals(1, records.size());
        assertEquals("/content/wknd", records.get(0).getPath());
        verify(resolver, never()).getResource(anyString());
    }

    @Test
    void purgeReturnsZeroWhenResolverThrowsPersistenceException() throws Exception {
        final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);

        final Resource auditRoot = Mockito.mock(Resource.class);
        when(resolver.getResource(IncrementalIndexingDefaults.AUDIT_ROOT)).thenReturn(auditRoot);

        final Resource child = Mockito.mock(Resource.class);
        final long cutoffMs =
                Instant.now().toEpochMilli() - IncrementalIndexingDefaults.AUDIT_RETENTION_MS;
        final long oldTimestampMs = cutoffMs - 1_000L;
        when(child.getValueMap()).thenReturn(
                new ValueMapDecorator(Map.of("timestampMs", oldTimestampMs)));
        when(auditRoot.getChildren()).thenReturn(Collections.singletonList(child));

        Mockito.doThrow(new PersistenceException("purge failed")).when(resolver).delete(child);

        assertEquals(0, auditService.purgeOlderThanRetention());
    }

    @Test
    void purgeReturnsZeroWhenResolverThrowsGenericException() throws Exception {
        when(resolverUtil.getServiceResolver()).thenThrow(new RuntimeException("boom"));

        assertEquals(0, auditService.purgeOlderThanRetention());
    }

    @Test
    void parseTimestampMsReturnsZeroForBlankInvalidAndMissingValues() throws Exception {
        final long recentTimestampMs = Instant.now().toEpochMilli();

        context.create().resource(
                IncrementalIndexingDefaults.AUDIT_ROOT + "/entry-blank",
                "timestampMs",
                recentTimestampMs,
                "timestamp",
                " ",
                "path",
                "/content/a",
                "action",
                IndexingAction.INDEX.name(),
                "status",
                "SUCCESS");

        context.create().resource(
                IncrementalIndexingDefaults.AUDIT_ROOT + "/entry-invalid",
                "timestampMs",
                recentTimestampMs,
                "timestamp",
                "not-a-timestamp",
                "path",
                "/content/b",
                "action",
                IndexingAction.INDEX.name(),
                "status",
                "SUCCESS");

        context.create().resource(
                IncrementalIndexingDefaults.AUDIT_ROOT + "/entry-missing",
                "timestampMs",
                recentTimestampMs,
                "path",
                "/content/c",
                "action",
                IndexingAction.INDEX.name(),
                "status",
                "SUCCESS");

        final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.findResources(anyString(), eq(Query.JCR_SQL2))).thenReturn(Collections.emptyIterator());
        when(resolver.getResource(IncrementalIndexingDefaults.AUDIT_ROOT))
                .thenReturn(context.resourceResolver().getResource(IncrementalIndexingDefaults.AUDIT_ROOT));

        final List<IndexingAuditRecord> records = auditService.getRecordsForLast24Hours();

        assertTrue(records.stream().anyMatch(record -> "/content/a".equals(record.getPath())));
        assertTrue(records.stream().anyMatch(record -> "/content/b".equals(record.getPath())));
        assertTrue(records.stream().anyMatch(record -> "/content/c".equals(record.getPath())));
    }
}
