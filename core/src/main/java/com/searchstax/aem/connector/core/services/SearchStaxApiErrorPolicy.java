package com.searchstax.aem.connector.core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Retry, backoff, and error-handling rules aligned with SearchStax API guidance.
 *
 * @see <a href="https://support.searchstax.com/hc/en-us/articles/44588170831117-Rate-Limits-and-Backoff">Rate Limits and Backoff</a>
 * @see <a href="https://support.searchstax.com/hc/en-us/articles/44588170626317-Error-Handling-and-HTTP-Status-Codes">Error Handling and HTTP Status Codes</a>
 */
public final class SearchStaxApiErrorPolicy {

    public static final int MAX_POST_ATTEMPTS = 5;

    public static final long BASE_BACKOFF_MS = 700L;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SearchStaxApiErrorPolicy() {
    }

    public static boolean isHttpSuccess(final int statusCode) {
        return statusCode == 204 || (statusCode >= 200 && statusCode < 300);
    }

    public static boolean isRetryable(final int statusCode) {
        return statusCode == 429
                || statusCode == 500
                || statusCode == 502
                || statusCode == 503
                || statusCode == 504;
    }

    public static boolean isNonRetryable(final int statusCode) {
        return statusCode == 400
                || statusCode == 401
                || statusCode == 403
                || statusCode == 404
                || statusCode == 413
                || statusCode == 414
                || statusCode == 422;
    }

    public static boolean shouldSplitBatch(final int statusCode) {
        return statusCode == 413;
    }

    /**
     * Exponential backoff for the given 1-based POST attempt index within the retry loop.
     * SearchStax guidance: increase delay with each retry; do not rely on Retry-After.
     */
    public static long backoffMillis(final int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be >= 1");
        }
        return BASE_BACKOFF_MS * (1L << (attempt - 1));
    }

    public static boolean isSuccessfulResponse(final int statusCode, final String responseBody) {
        if (!isHttpSuccess(statusCode)) {
            return false;
        }
        return !hasPayloadLevelFailure(responseBody);
    }

    public static boolean hasPayloadLevelFailure(final String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }

        try {
            final JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            if (root.has("success") && !root.get("success").asBoolean(true)) {
                return true;
            }
            if (root.has("error_message") && !root.get("error_message").asText("").isBlank()) {
                return true;
            }
            if (root.has("error") && root.get("error").isTextual() && !root.get("error").asText("").isBlank()) {
                return true;
            }
        } catch (Exception ignored) {
            // Non-JSON success bodies are treated as success when HTTP status is 2xx/204.
        }
        return false;
    }

    public static String resolveGuidanceMessage(final int statusCode, final String responseBody) {
        final String guidance;
        switch (statusCode) {
            case 400:
                guidance = "Request reached SearchStax but payload is invalid. Correct field mappings and document values.";
                break;
            case 401:
                guidance = "Missing or invalid update API token. Verify API configuration credentials.";
                break;
            case 403:
                guidance = "Token is not authorized for the update endpoint. Verify endpoint and token scope.";
                break;
            case 413:
                guidance = "Payload exceeds SearchStax 10 MB limit. Reduce batch size or document content.";
                break;
            case 414:
                guidance = "URL or request line exceeds SearchStax size limits (10 KB request / 10,240 URL chars).";
                break;
            case 422:
                guidance = "SearchStax validation failed on required inputs. Review mapped Solr fields.";
                break;
            case 429:
                guidance = "SearchStax rate or plan limit exceeded. Retries exhausted; verify request volume and plan usage.";
                break;
            case 500:
            case 502:
            case 503:
            case 504:
                guidance = "SearchStax service failure. Check plan usage and service limits; contact support if persistent.";
                break;
            default:
                guidance = "Unexpected SearchStax API response.";
                break;
        }

        final String payloadError = extractPayloadErrorMessage(responseBody);
        if (payloadError.isBlank()) {
            return guidance;
        }
        return guidance + " Response: " + payloadError;
    }

    public static String extractPayloadErrorMessage(final String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }

        try {
            final JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            if (root.has("error_message") && root.get("error_message").isTextual()) {
                return root.get("error_message").asText();
            }
            if (root.has("error") && root.get("error").isTextual()) {
                return root.get("error").asText();
            }
            if (root.has("message") && root.get("message").isTextual()) {
                return root.get("message").asText();
            }
        } catch (Exception ignored) {
            return truncate(responseBody, 300);
        }

        return truncate(responseBody, 300);
    }

    private static String truncate(final String value, final int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
