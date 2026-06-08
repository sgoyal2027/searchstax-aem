package com.searchstax.aem.connector.core.incremental;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexingScopeDecisionTest {

    @Test
    void acceptDecisionIsAcceptedWithoutReason() {
        final IndexingScopeDecision decision = IndexingScopeDecision.accept();

        assertTrue(decision.isAccepted());
        assertNull(decision.getReason());
    }

    @Test
    void rejectDecisionContainsReason() {
        final IndexingScopeDecision decision = IndexingScopeDecision.reject("Out of scope");

        assertTrue(decision.isAccepted() == false);
        assertEquals("Out of scope", decision.getReason());
    }
}
