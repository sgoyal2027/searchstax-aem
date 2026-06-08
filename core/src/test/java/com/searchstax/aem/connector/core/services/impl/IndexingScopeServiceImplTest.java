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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void rejectsWhenConfigIsNull() {
        when(initialSetupConfigService.getConfiguration()).thenReturn(null);

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/site/en/page");

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
    void rejectsPathOutsideConfiguredRoot() {
        when(initialSetupConfigService.getConfiguration()).thenReturn(baseConfig());

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/other/site/page");
        assertFalse(decision.isAccepted());
    }

    @Test
    void acceptsAllowedAssetMimeType() {
        final InitialSetupConfig config = baseConfig();
        config.setAllowedFiles(new String[] {"application/pdf"});
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

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/dam/site/sample.pdf");
        assertTrue(decision.isAccepted());
    }

    @Test
    void resolvesPageContentForDocumentBuilding() {
        context.create().page("/content/site/en/page");

        final Resource content = indexingScopeService.resolveIndexableResource(
                context.resourceResolver(),
                "/content/site/en/page");

        assertNotNull(content);
        assertEquals("/content/site/en/page/jcr:content", content.getPath());
    }

    @Test
    void resolvesPageResourceItselfWhenJcrContentMissing() {
        context.create().resource(
                "/content/site/en/page",
                "jcr:primaryType",
                "cq:Page");

        final Resource resolved = indexingScopeService.resolveIndexableResource(
                context.resourceResolver(),
                "/content/site/en/page");

        assertNotNull(resolved);
        assertEquals("/content/site/en/page", resolved.getPath());
    }

    @Test
    void rejectsEmptyPath() {
        when(initialSetupConfigService.getConfiguration()).thenReturn(baseConfig());

        final var decision = indexingScopeService.evaluate(context.resourceResolver(), "  ");
        assertFalse(decision.isAccepted());
    }

    @Test
    void rejectsWhenNoRootPathsConfigured() {
        final InitialSetupConfig config = baseConfig();
        config.setRootPaths(new String[0]);
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        final var decision = indexingScopeService.evaluate(context.resourceResolver(), "/content/site/page");
        assertFalse(decision.isAccepted());
    }

    @Test
    void rejectsMissingResourceOutsideDam() {
        when(initialSetupConfigService.getConfiguration()).thenReturn(baseConfig());

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/site/missing-page");
        assertFalse(decision.isAccepted());
    }

    @Test
    void acceptsDamPathWhenResourceNotYetCreated() {
        when(initialSetupConfigService.getConfiguration()).thenReturn(baseConfig());

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/dam/site/future-asset.pdf");
        assertTrue(decision.isAccepted());
    }

    @Test
    void rejectsUnsupportedNodeType() {
        when(initialSetupConfigService.getConfiguration()).thenReturn(baseConfig());
        context.create().resource("/content/site/folder", "jcr:primaryType", "sling:Folder");

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/site/folder");
        assertFalse(decision.isAccepted());
    }

    @Test
    void rejectsAssetWhenMimeTypeMissing() {
        final InitialSetupConfig config = baseConfig();
        config.setAllowedFiles(new String[] {"application/pdf"});
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        context.create().resource("/content/dam/site/no-mime.pdf", "jcr:primaryType", "dam:Asset");
        context.create().resource(
                "/content/dam/site/no-mime.pdf/jcr:content",
                "jcr:primaryType",
                "dam:AssetContent");

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/dam/site/no-mime.pdf");
        assertFalse(decision.isAccepted());
    }

    @Test
    void acceptsAssetMatchingWildcardExtension() {
        final InitialSetupConfig config = baseConfig();
        config.setAllowedFiles(new String[] {"*.pdf"});
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        context.create().resource("/content/dam/site/sample.pdf", "jcr:primaryType", "dam:Asset");
        context.create().resource(
                "/content/dam/site/sample.pdf/jcr:content",
                "jcr:primaryType",
                "dam:AssetContent",
                "jcr:mimeType",
                "application/pdf");

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/dam/site/sample.pdf");
        assertTrue(decision.isAccepted());
    }

    @Test
    void resolveIndexableResourceReturnsNullForBlankPath() {
        assertEquals(null, indexingScopeService.resolveIndexableResource(context.resourceResolver(), " "));
    }

    @Test
    void resolvesAssetMetadataResource() {
        context.create().resource("/content/dam/site/sample.pdf", "jcr:primaryType", "dam:Asset");
        context.create().resource(
                "/content/dam/site/sample.pdf/jcr:content",
                "jcr:primaryType",
                "dam:AssetContent");
        context.create().resource(
                "/content/dam/site/sample.pdf/jcr:content/metadata",
                "jcr:primaryType",
                "nt:unstructured",
                "dc:format",
                "application/pdf");

        final Resource metadata = indexingScopeService.resolveIndexableResource(
                context.resourceResolver(),
                "/content/dam/site/sample.pdf");

        assertNotNull(metadata);
        assertEquals("/content/dam/site/sample.pdf/jcr:content/metadata", metadata.getPath());
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

    @Test
    void acceptsAssetWhenMetadataDcFormatMatchesAllowedFiles() {
        final InitialSetupConfig config = baseConfig();
        config.setAllowedFiles(new String[] {"application/pdf"});
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        context.create().resource(
                "/content/dam/site/metadata-match.pdf",
                "jcr:primaryType",
                "dam:Asset");
        context.create().resource(
                "/content/dam/site/metadata-match.pdf/jcr:content/metadata",
                "jcr:primaryType",
                "nt:unstructured",
                "dc:format",
                "application/pdf");

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/dam/site/metadata-match.pdf");

        assertTrue(decision.isAccepted());
    }

    @Test
    void rejectsAssetWhenMetadataDcFormatBlank() {
        final InitialSetupConfig config = baseConfig();
        config.setAllowedFiles(new String[] {"application/pdf"});
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        context.create().resource(
                "/content/dam/site/metadata-blank.pdf",
                "jcr:primaryType",
                "dam:Asset");
        context.create().resource(
                "/content/dam/site/metadata-blank.pdf/jcr:content/metadata",
                "jcr:primaryType",
                "nt:unstructured",
                "dc:format",
                " ");

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/dam/site/metadata-blank.pdf");

        assertFalse(decision.isAccepted());
    }

    @Test
    void rejectsInvalidNormalizedPath() {
        when(initialSetupConfigService.getConfiguration()).thenReturn(baseConfig());

        final var decision = indexingScopeService.evaluate(context.resourceResolver(), "//");
        assertFalse(decision.isAccepted());
    }

    @Test
    void acceptsAssetWhenAllowedFilesNotConfigured() {
        when(initialSetupConfigService.getConfiguration()).thenReturn(baseConfig());
        context.create().resource("/content/dam/site/sample.pdf", "jcr:primaryType", "dam:Asset");

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/dam/site/sample.pdf");
        assertTrue(decision.isAccepted());
    }

    @Test
    void resolveIndexableResourceReturnsNullWhenResourceMissing() {
        assertEquals(null, indexingScopeService.resolveIndexableResource(
                context.resourceResolver(),
                "/content/site/missing"));
    }

    @Test
    void resolveIndexableResourceReturnsAssetContentWhenMetadataMissing() {
        context.create().resource("/content/dam/site/sample.pdf", "jcr:primaryType", "dam:Asset");
        context.create().resource(
                "/content/dam/site/sample.pdf/jcr:content",
                "jcr:primaryType",
                "dam:AssetContent",
                "jcr:mimeType",
                "application/pdf");

        final Resource resolved = indexingScopeService.resolveIndexableResource(
                context.resourceResolver(),
                "/content/dam/site/sample.pdf");

        assertNotNull(resolved);
        assertEquals("/content/dam/site/sample.pdf/jcr:content", resolved.getPath());
    }

    @Test
    void rejectsAssetWhenAllowedEntryIsBlank() {
        final InitialSetupConfig config = baseConfig();
        config.setAllowedFiles(new String[] {" ", "application/pdf"});
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        context.create().resource("/content/dam/site/sample.pdf", "jcr:primaryType", "dam:Asset");
        context.create().resource(
                "/content/dam/site/sample.pdf/jcr:content",
                "jcr:primaryType",
                "dam:AssetContent",
                "jcr:mimeType",
                "application/pdf");

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/dam/site/sample.pdf");
        assertTrue(decision.isAccepted());
    }

    @Test
    void acceptsPageResourceWithJcrContentChildEvenWithoutCqPageType() {
        when(initialSetupConfigService.getConfiguration()).thenReturn(baseConfig());
        context.create().resource("/content/site/page", "jcr:primaryType", "nt:unstructured");
        context.create().resource("/content/site/page/jcr:content", "jcr:primaryType", "cq:PageContent");

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/site/page");
        assertTrue(decision.isAccepted());
    }

    @Test
    void rejectsAssetWhenMimeTypeCannotBeResolved() {
        final InitialSetupConfig config = baseConfig();
        config.setAllowedFiles(new String[] {"application/pdf"});
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);
        context.create().resource("/content/dam/site/no-content.pdf", "jcr:primaryType", "dam:Asset");

        final var decision = indexingScopeService.evaluate(
                context.resourceResolver(),
                "/content/dam/site/no-content.pdf");
        assertFalse(decision.isAccepted());
    }

    @Test
    void hasNodeTypeReturnsFalseWhenRepositoryExceptionThrown() throws Exception {
        when(initialSetupConfigService.getConfiguration()).thenReturn(baseConfig());

        final org.apache.sling.api.resource.ResourceResolver resolver =
                org.mockito.Mockito.mock(org.apache.sling.api.resource.ResourceResolver.class);
        final Resource resource = org.mockito.Mockito.mock(Resource.class);
        final javax.jcr.Node node = org.mockito.Mockito.mock(javax.jcr.Node.class);

        when(resolver.getResource("/content/site/page")).thenReturn(resource);
        when(resource.getPath()).thenReturn("/content/site/page");
        when(resource.getChild("jcr:content")).thenReturn(null);
        when(resource.adaptTo(javax.jcr.Node.class)).thenReturn(node);
        when(node.isNodeType(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new javax.jcr.RepositoryException("node type unavailable"));

        final var decision = indexingScopeService.evaluate(resolver, "/content/site/page");
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
