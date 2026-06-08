package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchStaxApiErrorPolicyExtendedTest {

    @Test
    void backoffRequiresPositiveAttempt() {
        assertThrows(IllegalArgumentException.class, () -> SearchStaxApiErrorPolicy.backoffMillis(0));
    }

    @Test
    void detectsErrorFieldInPayload() {
        assertTrue(SearchStaxApiErrorPolicy.hasPayloadLevelFailure("{\"error\":\"invalid field\"}"));
    }

    @Test
    void resolveGuidanceForAllStatusCodes() {
        assertTrue(SearchStaxApiErrorPolicy.resolveGuidanceMessage(400, null).contains("payload"));
        assertTrue(SearchStaxApiErrorPolicy.resolveGuidanceMessage(403, null).contains("authorized"));
        assertTrue(SearchStaxApiErrorPolicy.resolveGuidanceMessage(414, null).contains("URL"));
        assertTrue(SearchStaxApiErrorPolicy.resolveGuidanceMessage(422, null).contains("validation"));
        assertTrue(SearchStaxApiErrorPolicy.resolveGuidanceMessage(502, null).contains("service failure"));
        assertTrue(SearchStaxApiErrorPolicy.resolveGuidanceMessage(999, null).contains("Unexpected"));
    }

    @Test
    void resolveGuidanceAppendsPayloadErrorMessage() {
        final String message = SearchStaxApiErrorPolicy.resolveGuidanceMessage(
                400,
                "{\"error_message\":\"title_txt_us missing\"}");
        assertTrue(message.contains("title_txt_us missing"));
    }

    @Test
    void extractPayloadErrorMessageFromJsonFields() {
        assertEquals("bad", SearchStaxApiErrorPolicy.extractPayloadErrorMessage("{\"error\":\"bad\"}"));
        assertEquals("msg", SearchStaxApiErrorPolicy.extractPayloadErrorMessage("{\"message\":\"msg\"}"));
    }

    @Test
    void extractPayloadErrorMessageExtractsErrorMessageField() {
        assertEquals(
                "boom",
                SearchStaxApiErrorPolicy.extractPayloadErrorMessage("{\"error_message\":\"boom\"}"));
    }

    @Test
    void extractPayloadErrorMessageReturnsEmptyForBlankBody() {
        assertEquals("", SearchStaxApiErrorPolicy.extractPayloadErrorMessage("   "));
    }

    @Test
    void extractPayloadErrorMessageTruncatesNonJsonBody() {
        final String longBody = "x".repeat(400);
        assertEquals(300, SearchStaxApiErrorPolicy.extractPayloadErrorMessage(longBody).length());
    }

    @Test
    void isHttpSuccessIncludes204() {
        assertTrue(SearchStaxApiErrorPolicy.isHttpSuccess(204));
        assertTrue(SearchStaxApiErrorPolicy.isHttpSuccess(200));
    }

    @Test
    void isNonRetryableIncludes404() {
        assertTrue(SearchStaxApiErrorPolicy.isNonRetryable(404));
    }

    @Test
    void hasPayloadLevelFailureWhenSuccessFalse() {
        assertTrue(SearchStaxApiErrorPolicy.hasPayloadLevelFailure("{\"success\":false}"));
    }

    @Test
    void hasPayloadLevelFailureTreatsInvalidJsonAsSuccess() {
        assertFalse(SearchStaxApiErrorPolicy.hasPayloadLevelFailure("not-json"));
    }

    @Test
    void extractPayloadErrorMessageReturnsTruncatedBodyForValidJsonWithoutErrorFields() {
        final String body = "{\"status\":\"ok\",\"detail\":\"" + "y".repeat(400) + "\"}";
        assertEquals(300, SearchStaxApiErrorPolicy.extractPayloadErrorMessage(body).length());
    }

    @Test
    void hasPayloadLevelFailureWhenErrorMessagePresent() {
        assertTrue(
                SearchStaxApiErrorPolicy.hasPayloadLevelFailure(
                        "{\"error_message\":\"invalid field\"}"));
    }
}
