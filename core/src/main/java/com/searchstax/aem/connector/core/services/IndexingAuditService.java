package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.incremental.IndexingAuditRecord;

import java.util.List;

public interface IndexingAuditService {

    void record(
            String path,
            IndexingAction action,
            String status,
            String batchId,
            int httpStatus,
            String message,
            long durationMs,
            String documentId);

    List<IndexingAuditRecord> getRecordsForLast24Hours();

    int purgeOlderThanRetention();
}
