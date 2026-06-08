package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.constants.SearchStaxServiceLimits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullIndexProgressTest {

    @Test
    void exposesProgressFieldsAndDerivedTotals() {
        final FullIndexProgress progress = new FullIndexProgress(
                FullIndexProgress.State.RUNNING,
                10L,
                8L,
                2L,
                6L,
                4L,
                3,
                "/content/wknd/us/en/page",
                1000L,
                5000L,
                "Running");

        assertEquals(FullIndexProgress.State.RUNNING, progress.getState());
        assertEquals(10L, progress.getTotalProcessed());
        assertEquals(10L, progress.getTotalAttempted());
        assertEquals("Running", progress.getMessage());
    }
}

class SearchStaxFullIndexRunResultTest {

    @Test
    void normalizesNullMessageAndBody() {
        final SearchStaxFullIndexRunResult result =
                new SearchStaxFullIndexRunResult(true, null, 200, 5, 2, null);

        assertTrue(result.isSuccess());
        assertEquals("", result.getMessage());
        assertEquals("", result.getSolrResponseBody());
        assertEquals(5, result.getPagesIndexed());
        assertEquals(2, result.getAssetsIndexed());
        assertEquals(200, result.getHttpStatus());
    }
}

class FullIndexTriggerResultTest {

    @Test
    void exposesAcceptedState() {
        final FullIndexTriggerResult result = new FullIndexTriggerResult(true, "job-1", "Accepted", 202);

        assertTrue(result.isAccepted());
        assertEquals("job-1", result.getJobId());
        assertEquals(202, result.getHttpStatus());
    }
}

class IncrementalIndexingDefaultsTest {

    @Test
    void exposesConstants() {
        assertEquals("searchstax/incremental", com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults.JOB_TOPIC);
        assertTrue(com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults.DEBOUNCE_MS > 0);
    }
}

class SearchStaxFullIndexDefaultsTest {

    @Test
    void exposesTraversalModeValues() {
        assertEquals(
                SearchStaxFullIndexDefaults.TraversalMode.JCR_SQL2,
                SearchStaxFullIndexDefaults.TRAVERSAL_MODE);
    }
}

class SearchStaxServiceLimitsTest {

    @Test
    void exposesServiceLimits() {
        assertTrue(com.searchstax.aem.connector.core.constants.SearchStaxServiceLimits.MAX_DOCUMENT_BYTES > 0);
        assertTrue(SearchStaxServiceLimits.MAX_PAYLOAD_BYTES > 0);
    }
}

class SearchStaxWizardBindingPathsTest {

    @Test
    void exposesWizardServletPaths() {
        assertTrue(com.searchstax.aem.connector.core.config.wizard.SearchStaxWizardBindingPaths.SERVLET_FULL_INDEX_RUN
                .endsWith("/fullindex-run"));
    }
}
