package com.searchstax.aem.connector.core.listeners;

import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.services.IncrementalIndexingQueueService;
import com.searchstax.aem.connector.core.services.IndexingScopeService;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;

import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class ReplicationIndexingEventHandlerTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private IndexingScopeService indexingScopeService;

    @Mock
    private IncrementalIndexingQueueService incrementalIndexingQueueService;

    private ReplicationIndexingEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new ReplicationIndexingEventHandler();
        context.registerService(IndexingScopeService.class, indexingScopeService);
        context.registerService(IncrementalIndexingQueueService.class, incrementalIndexingQueueService);
        context.registerInjectActivateService(eventHandler);
    }

    @Test
    void ignoresEventsWhenConnectorDisabled() {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(false);

        eventHandler.handleEvent(new Event(
                "com/day/cq/replication/action",
                Map.of("path", "/content/site/page", "type", "ACTIVATE")));

        verify(incrementalIndexingQueueService, never()).enqueue("/content/site/page", IndexingAction.INDEX);
    }

    @Test
    void enqueuesIndexActionForActivateEvent() {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);

        eventHandler.handleEvent(new Event(
                "com/day/cq/replication/action",
                Map.of("path", "/content/site/page", "type", "ACTIVATE")));

        verify(incrementalIndexingQueueService).enqueue("/content/site/page", IndexingAction.INDEX);
    }

    @Test
    void enqueuesDeleteActionForDeactivateEvent() {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);

        eventHandler.handleEvent(new Event(
                "org/apache/sling/distribution/agent/package/distributed",
                Map.of(
                        "distribution.paths",
                        new String[] {"/content/site/page"},
                        "distribution.type",
                        "DEACTIVATE")));

        verify(incrementalIndexingQueueService).enqueue("/content/site/page", IndexingAction.DELETE);
    }
}
