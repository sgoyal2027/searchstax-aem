package com.searchstax.aem.connector.core.dto.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexingBatchResultTest {

    @Test
    void exposesBatchOutcomeFields() {
        final IndexingBatchResult result = new IndexingBatchResult(true, 200, "Indexed documents", 3, 42L);

        assertTrue(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertEquals("Indexed documents", result.getMessage());
        assertEquals(3, result.getItemCount());
        assertEquals(42L, result.getDurationMs());
    }

    @Test
    void representsFailureOutcome() {
        final IndexingBatchResult result = new IndexingBatchResult(false, 500, "Server error", 1, 5L);

        assertFalse(result.isSuccess());
        assertEquals(500, result.getStatusCode());
        assertEquals("Server error", result.getMessage());
    }
}
