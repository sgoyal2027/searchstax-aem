package com.searchstax.aem.connector.core.services;

/**
 * Retry rules for SearchStax update API calls. Delegates to {@link SearchStaxApiErrorPolicy}.
 */
public final class SearchStaxFullIndexRetryPolicy {

    public static final int MAX_POST_ATTEMPTS = SearchStaxApiErrorPolicy.MAX_POST_ATTEMPTS;

    public static final long BASE_BACKOFF_MS = SearchStaxApiErrorPolicy.BASE_BACKOFF_MS;

    private SearchStaxFullIndexRetryPolicy() {
    }

    public static boolean isRetryable(final int statusCode) {
        return SearchStaxApiErrorPolicy.isRetryable(statusCode);
    }

    public static boolean isNonRetryable(final int statusCode) {
        return SearchStaxApiErrorPolicy.isNonRetryable(statusCode);
    }

    public static long backoffMillis(final int attempt) {
        return SearchStaxApiErrorPolicy.backoffMillis(attempt);
    }
}
