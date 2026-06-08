package com.searchstax.aem.connector.core.listeners;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import org.apache.sling.api.resource.LoginException;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.incremental.IndexingScopeDecision;
import com.searchstax.aem.connector.core.services.IncrementalIndexingQueueService;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.IndexingScopeService;
import com.searchstax.aem.connector.core.services.SearchStaxDocumentBuilderService;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchStaxReplicationPreprocessorTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private IndexingScopeService indexingScopeService;

    @Mock
    private IncrementalIndexingQueueService incrementalIndexingQueueService;

    @Mock
    private IndexingAuditService indexingAuditService;

    @Mock
    private SearchStaxDocumentBuilderService documentBuilderService;

    private SearchStaxReplicationPreprocessor preprocessor;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());

        preprocessor = new SearchStaxReplicationPreprocessor();
        context.registerService(ResolverUtil.class, resolverUtil);
        context.registerService(IndexingScopeService.class, indexingScopeService);
        context.registerService(IncrementalIndexingQueueService.class, incrementalIndexingQueueService);
        context.registerService(IndexingAuditService.class, indexingAuditService);
        context.registerService(SearchStaxDocumentBuilderService.class, documentBuilderService);
        context.registerInjectActivateService(preprocessor);
    }

    @Test
    void queuesOnlyInScopeReplicationActivatePaths() throws Exception {
        final ReplicationAction action = mock(ReplicationAction.class);
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(action.getType()).thenReturn(ReplicationActionType.ACTIVATE);
        when(action.getPaths()).thenReturn(new String[] {
                "/content/wknd/us/en/magazine/ski-touring",
                "/conf/wknd-shared/settings/dam/cfm/models/article",
                "/content/dam/wknd-shared/en/magazine/sample.jpg"
        });
        when(indexingScopeService.evaluate(any(), eq("/content/wknd/us/en/magazine/ski-touring")))
                .thenReturn(IndexingScopeDecision.accept());
        when(indexingScopeService.evaluate(any(), eq("/conf/wknd-shared/settings/dam/cfm/models/article")))
                .thenReturn(IndexingScopeDecision.reject("outside configured root paths"));
        when(indexingScopeService.evaluate(any(), eq("/content/dam/wknd-shared/en/magazine/sample.jpg")))
                .thenReturn(IndexingScopeDecision.reject("asset MIME type not allowed"));
        when(documentBuilderService.resolveDocumentId("/content/wknd/us/en/magazine/ski-touring"))
                .thenReturn("/content/wknd/us/en/magazine/ski-touring");

        preprocessor.preprocess(action, null);

        verify(incrementalIndexingQueueService).enqueue(
                "/content/wknd/us/en/magazine/ski-touring",
                IndexingAction.INDEX);
        verify(incrementalIndexingQueueService, never()).enqueue(
                eq("/conf/wknd-shared/settings/dam/cfm/models/article"),
                eq(IndexingAction.INDEX));
        verify(indexingAuditService).record(
                eq("/content/wknd/us/en/magazine/ski-touring"),
                eq(IndexingAction.INDEX),
                eq("QUEUED"),
                eq("replication"),
                eq(0),
                anyString(),
                anyLong(),
                eq("/content/wknd/us/en/magazine/ski-touring"));
        verify(indexingAuditService, never()).record(
                eq("/conf/wknd-shared/settings/dam/cfm/models/article"),
                any(),
                anyString(),
                anyString(),
                any(Integer.class),
                anyString(),
                anyLong(),
                anyString());
    }

    @Test
    void stillQueuesDeactivatePathsWithoutScopeFilter() throws Exception {
        final ReplicationAction action = mock(ReplicationAction.class);
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(action.getType()).thenReturn(ReplicationActionType.DEACTIVATE);
        when(action.getPaths()).thenReturn(new String[] {"/content/experience-fragments/wknd/en/site/footer"});
        when(documentBuilderService.resolveDocumentId("/content/experience-fragments/wknd/en/site/footer"))
                .thenReturn("/content/experience-fragments/wknd/en/site/footer");

        preprocessor.preprocess(action, null);

        verify(incrementalIndexingQueueService).enqueue(
                "/content/experience-fragments/wknd/en/site/footer",
                IndexingAction.DELETE);
        verify(indexingScopeService, never()).evaluate(any(), anyString());
    }

    @Test
    void ignoresNullReplicationAction() throws Exception {
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);

        preprocessor.preprocess(null, null);

        verify(incrementalIndexingQueueService, never()).enqueue(anyString(), any());
    }

    @Test
    void ignoresUnsupportedReplicationTypes() throws Exception {
        final ReplicationAction action = mock(ReplicationAction.class);
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(action.getType()).thenReturn(ReplicationActionType.TEST);
        when(action.getPaths()).thenReturn(new String[] {"/content/wknd/page"});

        preprocessor.preprocess(action, null);

        verify(incrementalIndexingQueueService, never()).enqueue(anyString(), any());
    }

    @Test
    void queuesDeleteReplicationAction() throws Exception {
        final ReplicationAction action = mock(ReplicationAction.class);
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(action.getType()).thenReturn(ReplicationActionType.DELETE);
        when(action.getPaths()).thenReturn(new String[] {"/content/wknd/page"});
        when(documentBuilderService.resolveDocumentId("/content/wknd/page")).thenReturn("/content/wknd/page");

        preprocessor.preprocess(action, null);

        verify(incrementalIndexingQueueService).enqueue("/content/wknd/page", IndexingAction.DELETE);
    }

    @Test
    void skipsQueueWhenServiceLoginFails() throws Exception {
        final ReplicationAction action = mock(ReplicationAction.class);
        when(indexingScopeService.isConnectorEnabled()).thenReturn(true);
        when(action.getType()).thenReturn(ReplicationActionType.ACTIVATE);
        when(action.getPaths()).thenReturn(new String[] {"/content/wknd/page"});
        when(resolverUtil.getServiceResolver()).thenThrow(new LoginException("login failed"));

        preprocessor.preprocess(action, null);

        verify(incrementalIndexingQueueService, never()).enqueue(anyString(), any());
    }

    @Test
    void ignoresReplicationWhenConnectorDisabled() throws Exception {
        final ReplicationAction action = mock(ReplicationAction.class);
        when(indexingScopeService.isConnectorEnabled()).thenReturn(false);

        preprocessor.preprocess(action, null);

        verify(incrementalIndexingQueueService, never()).enqueue(anyString(), any());
    }
}
