package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchStaxFullIndexRetryPolicyTest {

    @Test
    void delegatesRetryableStatusCodesToApiErrorPolicy() {
        assertTrue(SearchStaxFullIndexRetryPolicy.isRetryable(429));
        assertTrue(SearchStaxFullIndexRetryPolicy.isRetryable(500));
        assertTrue(SearchStaxFullIndexRetryPolicy.isNonRetryable(400));
    }

    @Test
    void calculatesExponentialBackoff() {
        assertEquals(SearchStaxApiErrorPolicy.BASE_BACKOFF_MS, SearchStaxFullIndexRetryPolicy.backoffMillis(1));
        assertTrue(SearchStaxFullIndexRetryPolicy.backoffMillis(2) > SearchStaxFullIndexRetryPolicy.backoffMillis(1));
    }
}
