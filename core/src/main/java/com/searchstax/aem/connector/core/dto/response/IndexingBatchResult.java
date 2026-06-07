package com.searchstax.aem.connector.core.dto.response;

public final class IndexingBatchResult {

    private final boolean success;
    private final int statusCode;
    private final String message;
    private final int itemCount;
    private final long durationMs;

    public IndexingBatchResult(
            final boolean success,
            final int statusCode,
            final String message,
            final int itemCount,
            final long durationMs) {
        this.success = success;
        this.statusCode = statusCode;
        this.message = message;
        this.itemCount = itemCount;
        this.durationMs = durationMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public int getItemCount() {
        return itemCount;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
