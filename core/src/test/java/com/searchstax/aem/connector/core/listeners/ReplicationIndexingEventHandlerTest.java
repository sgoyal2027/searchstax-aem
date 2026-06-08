package com.searchstax.aem.connector.core.listeners;

import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.incremental.IndexingScopeDecision;
import com.searchstax.aem.connector.core.services.IncrementalIndexingQueueService;
import com.searchstax.aem.connector.core.services.IndexingScopeService;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.sling.api.resource.LoginException;
import org.osgi.service.event.Event;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class ReplicationIndexingEventHandlerTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private IndexingScopeService indexingScopeService;

    @Mock
    private IncrementalIndexingQueueService incrementalIndexingQueueService;

    private ReplicationIndexingEventHandler eventHandler;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());

        eventHandler = new ReplicationIndexingEventHandler();
        context.registerService(ResolverUtil.class, resolverUtil);
        context.registerService(IndexingScopeService.class, indexingScopeService);
        context.registerService(IncrementalIndexingQueueService.class, incrementalIndexingQueueService);
        context.registerInjectActivateService(eventHandler);
    }

    @Test
    void ignoresEventsWhenConnectorDisabled() {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(false);

        eventHandler.handleEvent(new Event(
                "org/apache/sling/distribution/agent/package/distributed",
                Map.of(
                        "distribution.paths",
                        new String[] {"/content/wknd/us/en/page"},
                        "distribution.type",
                        "ACTIVATE")));

        verify(incrementalIndexingQueueService, never()).enqueue("/content/wknd/us/en/page", IndexingAction.INDEX);
    }

    @Test
    void enqueuesOnlyInScopeDistributionActivatePaths() {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(indexingScopeService.evaluate(any(), eq("/content/wknd/us/en/page")))
                .thenReturn(IndexingScopeDecision.accept());
        when(indexingScopeService.evaluate(any(), eq("/conf/wknd-shared/settings")))
                .thenReturn(IndexingScopeDecision.reject("outside configured root paths"));

        eventHandler.handleEvent(new Event(
                "org/apache/sling/distribution/agent/package/distributed",
                Map.of(
                        "distribution.paths",
                        new String[] {"/content/wknd/us/en/page", "/conf/wknd-shared/settings"},
                        "distribution.type",
                        "ACTIVATE")));

        verify(incrementalIndexingQueueService).enqueue("/content/wknd/us/en/page", IndexingAction.INDEX);
        verify(incrementalIndexingQueueService, never()).enqueue("/conf/wknd-shared/settings", IndexingAction.INDEX);
    }

    @Test
    void ignoresUnknownEventTypes() {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);

        eventHandler.handleEvent(new Event(
                "org/apache/sling/distribution/agent/package/distributed",
                Map.of(
                        "distribution.paths",
                        new String[] {"/content/wknd/us/en/page"},
                        "distribution.type",
                        "TEST")));

        verify(incrementalIndexingQueueService, never()).enqueue(any(), any());
    }

    @Test
    void ignoresEventsWithoutPaths() {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);

        eventHandler.handleEvent(new Event(
                "org/apache/sling/distribution/agent/package/distributed",
                Collections.singletonMap("distribution.type", "ACTIVATE")));

        verify(incrementalIndexingQueueService, never()).enqueue(any(), any());
    }

    @Test
    void skipsQueueWhenServiceLoginFails() throws Exception {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(resolverUtil.getServiceResolver()).thenThrow(new LoginException("login failed"));

        eventHandler.handleEvent(new Event(
                "org/apache/sling/distribution/agent/package/distributed",
                Map.of(
                        "distribution.paths",
                        new String[] {"/content/wknd/us/en/page"},
                        "distribution.type",
                        "ACTIVATE")));

        verify(incrementalIndexingQueueService, never()).enqueue(any(), any());
    }

    @Test
    void enqueuesQueuedJobPaths() {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(indexingScopeService.evaluate(any(), eq("/content/wknd/us/en/page")))
                .thenReturn(IndexingScopeDecision.accept());

        eventHandler.handleEvent(new Event(
                "org/apache/sling/distribution/agent/package/distributed",
                Map.of(
                        "paths",
                        new String[] {"/content/wknd/us/en/page"},
                        "type",
                        "ADD")));

        verify(incrementalIndexingQueueService).enqueue("/content/wknd/us/en/page", IndexingAction.INDEX);
    }

    @Test
    void enqueuesDeleteActionForDistributionDeactivateWithoutScopeFilter() {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);

        eventHandler.handleEvent(new Event(
                "org/apache/sling/distribution/agent/package/distributed",
                Map.of(
                        "distribution.paths",
                        new String[] {"/content/experience-fragments/wknd/en/site/footer"},
                        "distribution.type",
                        "DEACTIVATE")));

        verify(incrementalIndexingQueueService).enqueue(
                "/content/experience-fragments/wknd/en/site/footer",
                IndexingAction.DELETE);
        verify(indexingScopeService, never()).evaluate(any(), any());
    }
}
