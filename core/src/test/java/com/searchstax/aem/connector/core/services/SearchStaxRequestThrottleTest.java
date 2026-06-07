package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchStaxRequestThrottleTest {

    @AfterEach
    void tearDown() {
        SearchStaxRequestThrottle.resetForTests();
    }

    @Test
    void enforcesMinimumIntervalBetweenRequests() {
        SearchStaxRequestThrottle.configureMinIntervalMs(50L);

        final long started = System.currentTimeMillis();
        SearchStaxRequestThrottle.awaitTurn();
        SearchStaxRequestThrottle.awaitTurn();
        final long elapsed = System.currentTimeMillis() - started;

        assertTrue(elapsed >= 50L);
    }
}
