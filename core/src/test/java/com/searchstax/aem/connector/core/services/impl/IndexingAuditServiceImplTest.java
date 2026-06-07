package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.incremental.IndexingAuditRecord;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
}
