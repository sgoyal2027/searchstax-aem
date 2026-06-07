package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchStaxApiErrorPolicyTest {

    @Test
    void treats429And500AsRetryable() {
        assertTrue(SearchStaxApiErrorPolicy.isRetryable(429));
        assertTrue(SearchStaxApiErrorPolicy.isRetryable(500));
    }

    @Test
    void treats413And414And422AsNonRetryable() {
        assertTrue(SearchStaxApiErrorPolicy.isNonRetryable(413));
        assertTrue(SearchStaxApiErrorPolicy.isNonRetryable(414));
        assertTrue(SearchStaxApiErrorPolicy.isNonRetryable(422));
        assertFalse(SearchStaxApiErrorPolicy.isRetryable(413));
    }

    @Test
    void detectsPayloadLevelFailureAtHttp200() {
        assertFalse(SearchStaxApiErrorPolicy.isSuccessfulResponse(200, "{\"success\":false,\"error_message\":\"bad field\"}"));
        assertTrue(SearchStaxApiErrorPolicy.isSuccessfulResponse(200, "{\"success\":true}"));
        assertTrue(SearchStaxApiErrorPolicy.isSuccessfulResponse(204, ""));
    }

    @Test
    void usesExponentialBackoffWithoutRetryAfter() {
        assertEquals(700L, SearchStaxApiErrorPolicy.backoffMillis(1));
        assertEquals(1400L, SearchStaxApiErrorPolicy.backoffMillis(2));
    }

    @Test
    void providesGuidanceForRateLimitAndPayloadErrors() {
        assertTrue(SearchStaxApiErrorPolicy.resolveGuidanceMessage(429, null).contains("rate"));
        assertTrue(SearchStaxApiErrorPolicy.resolveGuidanceMessage(413, null).contains("10 MB"));
        assertTrue(SearchStaxApiErrorPolicy.resolveGuidanceMessage(401, null).contains("token"));
    }

    @Test
    void splitsBatchOnlyFor413() {
        assertTrue(SearchStaxApiErrorPolicy.shouldSplitBatch(413));
        assertFalse(SearchStaxApiErrorPolicy.shouldSplitBatch(429));
    }
}
