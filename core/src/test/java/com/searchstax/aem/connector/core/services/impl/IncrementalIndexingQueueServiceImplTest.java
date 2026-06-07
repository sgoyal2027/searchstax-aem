package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.event.jobs.JobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class IncrementalIndexingQueueServiceImplTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private JobManager jobManager;

    private IncrementalIndexingQueueServiceImpl queueService;

    @BeforeEach
    void setUp() {
        context.registerService(JobManager.class, jobManager);
        queueService = context.registerInjectActivateService(new IncrementalIndexingQueueServiceImpl());
    }

    @Test
    void deduplicatesPathsKeepingLastActionWhenBatchFlushes() {
        for (int index = 0; index < SearchStaxFullIndexDefaults.BATCH_SIZE - 1; index++) {
            queueService.enqueue("/content/site/page-" + index, IndexingAction.INDEX);
        }

        queueService.enqueue("/content/site/page-1", IndexingAction.DELETE);
        queueService.enqueue("/content/site/page-last", IndexingAction.INDEX);

        final ArgumentCaptor<Map<String, Object>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager, times(1)).addJob(eq(IncrementalIndexingDefaults.JOB_TOPIC), propertiesCaptor.capture());

        final Map<String, Object> properties = propertiesCaptor.getValue();
        final String[] indexPaths = (String[]) properties.get(IncrementalIndexingDefaults.JOB_PROP_INDEX_PATHS);
        final String[] deletePaths = (String[]) properties.get(IncrementalIndexingDefaults.JOB_PROP_DELETE_PATHS);

        assertEquals(SearchStaxFullIndexDefaults.BATCH_SIZE - 1, indexPaths.length);
        assertArrayEquals(new String[] {"/content/site/page-1"}, deletePaths);
    }
}
