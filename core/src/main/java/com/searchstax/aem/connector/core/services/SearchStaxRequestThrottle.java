package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.constants.SearchStaxServiceLimits;

/**
 * Simple process-wide throttle to stay under SearchStax request-rate limits.
 */
public final class SearchStaxRequestThrottle {

    private static final Object LOCK = new Object();

    private static volatile long minIntervalMs = SearchStaxServiceLimits.DEFAULT_MIN_REQUEST_INTERVAL_MS;

    private static volatile long lastRequestAtMs;

    private SearchStaxRequestThrottle() {
    }

    public static void configureMinIntervalMs(final long intervalMs) {
        if (intervalMs > 0) {
            minIntervalMs = intervalMs;
        }
    }

    public static void awaitTurn() {
        synchronized (LOCK) {
            final long now = System.currentTimeMillis();
            final long waitMs = minIntervalMs - (now - lastRequestAtMs);
            if (waitMs > 0) {
                sleep(waitMs);
            }
            lastRequestAtMs = System.currentTimeMillis();
        }
    }

    public static void resetForTests() {
        synchronized (LOCK) {
            minIntervalMs = SearchStaxServiceLimits.DEFAULT_MIN_REQUEST_INTERVAL_MS;
            lastRequestAtMs = 0L;
        }
    }

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
