package com.searchstax.aem.connector.core.constants;

/**
 * SearchStax Site Search service limits.
 *
 * @see <a href="https://support.searchstax.com/hc/en-us/articles/40049503450253-Service-Limits">Service Limits</a>
 */
public final class SearchStaxServiceLimits {

    /** Maximum document size accepted by SearchStax. */
    public static final int MAX_DOCUMENT_BYTES = 100 * 1024;

    /** Maximum request payload size. */
    public static final int MAX_PAYLOAD_BYTES = 10 * 1024 * 1024;

    /** Maximum request/query line size. */
    public static final int MAX_REQUEST_BYTES = 10 * 1024;

    /** Maximum update URL length including query parameters. */
    public static final int MAX_URL_LENGTH = 10_240;

    /**
     * Conservative minimum interval between update API calls for production apps (20+ req/sec limit).
     * Sandbox apps are limited to 5 req/sec; use OSGi override if needed.
     */
    public static final long DEFAULT_MIN_REQUEST_INTERVAL_MS = 100L;

    private SearchStaxServiceLimits() {
    }
}
