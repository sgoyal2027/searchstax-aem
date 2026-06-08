package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.services.impl.SearchStaxFullIndexRunServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SearchStaxFullIndexRunServiceImplTest {

    private final SearchStaxFullIndexRunServiceImpl service = new SearchStaxFullIndexRunServiceImpl();

    @Test
    void triggerFullIndexReturnsNotAvailableResponse() {
        final FullIndexTriggerResult result =
                service.triggerFullIndex(FullIndexPathConfig.fromDefaults());

        assertFalse(result.isAccepted());
        assertEquals(503, result.getHttpStatus());
    }

    @Test
    void getProgressReturnsIdleState() {
        assertEquals(FullIndexProgress.State.IDLE, service.getProgress().getState());
    }
}
