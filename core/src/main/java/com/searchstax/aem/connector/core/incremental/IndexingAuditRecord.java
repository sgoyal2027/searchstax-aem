package com.searchstax.aem.connector.core.incremental;

public final class IndexingAuditRecord {

    private final String timestamp;
    private final String path;
    private final IndexingAction action;
    private final String status;
    private final String batchId;
    private final int httpStatus;
    private final String message;
    private final long durationMs;
    private final String documentId;

    public IndexingAuditRecord(
            final String timestamp,
            final String path,
            final IndexingAction action,
            final String status,
            final String batchId,
            final int httpStatus,
            final String message,
            final long durationMs,
            final String documentId) {
        this.timestamp = timestamp;
        this.path = path;
        this.action = action;
        this.status = status;
        this.batchId = batchId;
        this.httpStatus = httpStatus;
        this.message = message;
        this.durationMs = durationMs;
        this.documentId = documentId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

    public IndexingAction getAction() {
        return action;
    }

    public String getStatus() {
        return status;
    }

    public String getBatchId() {
        return batchId;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getDocumentId() {
        return documentId;
    }
}
