package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import com.searchstax.aem.connector.core.services.IndexingScopeService;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class IndexingScopeServiceImplTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private InitialSetupConfigService initialSetupConfigService;

    private IndexingScopeService indexingScopeService;

    @BeforeEach
    void setUp() {
        indexingScopeService = new IndexingScopeServiceImpl();
        context.registerService(InitialSetupConfigService.class, initialSetupConfigService);
        context.registerInjectActivateService(indexingScopeService);
    }

    @Test
    void rejectsWhenConnectorDisabled() {
        final InitialSetupConfig config = baseConfig();
        config.setEnableConnector(false);
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        final var decision = indexingScopeService.evaluate(context.resourceResolver(), "/content/site/en/page");
        assertFalse(decision.isAccepted());
    }

    @Test
    void acceptsPageUnderConfiguredRoot() {
        when(initialSetupConfigService.getConfiguration()).thenReturn(baseConfig());
        context.create().page("/content/site/en/page");

        final var decision = indexingScopeService.evaluate(context.resourceResolver(), "/content/site/en/page");
        assertTrue(decision.isAccepted());
    }

    @Test
    void rejectsExcludedPath() {
        final InitialSetupConfig config = baseConfig();
        config.setExcludePaths(new String[] {"/content/site/en/private"});
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);
        context.create().page("/content/site/en/private/page");

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/site/en/private/page");
        assertFalse(decision.isAccepted());
    }

    @Test
    void rejectsAssetWithDisallowedMimeType() {
        final InitialSetupConfig config = baseConfig();
        config.setAllowedFiles(new String[] {"image/jpeg"});
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        context.create().resource(
                "/content/dam/site/sample.pdf",
                "jcr:primaryType",
                "dam:Asset");
        context.create().resource(
                "/content/dam/site/sample.pdf/jcr:content",
                "jcr:primaryType",
                "dam:AssetContent",
                "jcr:mimeType",
                "application/pdf");
        context.create().resource(
                "/content/dam/site/sample.pdf/jcr:content/metadata",
                "jcr:primaryType",
                "nt:unstructured",
                "dc:format",
                "application/pdf");

        final Resource asset = context.resourceResolver().getResource("/content/dam/site/sample.pdf");
        final var decision = indexingScopeService.evaluate(context.resourceResolver(), asset.getPath());
        assertFalse(decision.isAccepted());
    }

    private InitialSetupConfig baseConfig() {
        final InitialSetupConfig config = new InitialSetupConfig();
        config.setEnableConnector(true);
        config.setRootPaths(new String[] {"/content/site", "/content/dam"});
        config.setExcludePaths(new String[] {});
        config.setAllowedFiles(new String[] {});
        return config;
    }
}
