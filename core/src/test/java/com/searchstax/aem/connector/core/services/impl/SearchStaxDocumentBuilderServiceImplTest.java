package com.searchstax.aem.connector.core.services.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchstax.aem.connector.core.config.LanguageConfigService;
import com.searchstax.aem.connector.core.config.MetadataFieldConfigService;
import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.services.IndexingScopeService;
import com.searchstax.aem.connector.core.services.SearchStaxDocumentBuilderService;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class SearchStaxDocumentBuilderServiceImplTest {

    private static final String PAGE_PATH = "/content/wknd/us/en/magazine/arctic-surfing";

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private MetadataFieldConfigService metadataFieldConfigService;

    @Mock
    private LanguageConfigService languageConfigService;

    @Mock
    private IndexingScopeService indexingScopeService;

    private SearchStaxDocumentBuilderService documentBuilderService;

    @BeforeEach
    void setUp() {
        documentBuilderService = new SearchStaxDocumentBuilderServiceImpl();
        context.registerService(MetadataFieldConfigService.class, metadataFieldConfigService);
        context.registerService(LanguageConfigService.class, languageConfigService);
        context.registerService(IndexingScopeService.class, indexingScopeService);
        context.registerInjectActivateService(documentBuilderService);
    }

    @Test
    void buildsDocumentWithConfiguredLanguageSuffix() {
        stubMetadataMappings();
        final Resource content = createPageContent("Arctic Surfing", "Northern Norway surf story");
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH))).thenReturn(content);
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.of("en"));

        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                PAGE_PATH);

        assertTrue(document.isPresent());
        assertEquals(PAGE_PATH, document.get().get(IncrementalIndexingDefaults.DOCUMENT_ID_FIELD).asText());
        assertEquals("en", document.get().get(IncrementalIndexingDefaults.LANGUAGE_FIELD).asText());
        assertEquals("Arctic Surfing", document.get().get("title_txt_en").asText());
        assertEquals("Northern Norway surf story", document.get().get("description_txt_en").asText());
        assertTrue(document.get().has("created_dt"));
        assertFalse(document.get().has("created_dt_en"));
    }

    @Test
    void usesSearchStaxSuffixFromLanguageMappingConfig() {
        stubMetadataMappings();
        final Resource content = createPageContent("Arctic Surfing", "Northern Norway surf story");
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH))).thenReturn(content);
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.of("abc"));

        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                PAGE_PATH);

        assertTrue(document.isPresent());
        assertEquals("abc", document.get().get(IncrementalIndexingDefaults.LANGUAGE_FIELD).asText());
        assertEquals("Arctic Surfing", document.get().get("title_txt_abc").asText());
    }

    @Test
    void skipsDocumentWhenLanguageMappingMissing() {
        final Resource content = createPageContent("Arctic Surfing", "Northern Norway surf story");
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH))).thenReturn(content);
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.empty());

        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                PAGE_PATH);

        assertTrue(document.isEmpty());
    }

    @Test
    void buildsDeletePayloadWithDocumentId() {
        final Optional<ObjectNode> deletePayload = documentBuilderService.buildDeletePayload(PAGE_PATH);

        assertTrue(deletePayload.isPresent());
        assertEquals(PAGE_PATH, deletePayload.get().get(IncrementalIndexingDefaults.DOCUMENT_ID_FIELD).asText());
    }

    private Resource createPageContent(final String title, final String description) {
        context.create().resource(
                PAGE_PATH,
                "jcr:primaryType",
                "cq:Page");
        context.create().resource(
                PAGE_PATH + "/jcr:content",
                "jcr:primaryType",
                "cq:PageContent",
                "jcr:language",
                "en",
                "jcr:title",
                title,
                "jcr:description",
                description,
                "cq:tags",
                new String[] {"wknd:activity/surfing"},
                "jcr:created",
                Calendar.getInstance());
        return context.resourceResolver().getResource(PAGE_PATH + "/jcr:content");
    }

    private void stubMetadataMappings() {
        when(metadataFieldConfigService.getMetadataFieldMappings()).thenReturn(standardMappings());
    }

    private List<MetadataFieldMappingConfig> standardMappings() {
        return Arrays.asList(
                mapping("jcr:title", "title", "text"),
                mapping("jcr:description", "description", "text"),
                mapping("cq:tags", "tags", "strings"),
                mapping("jcr:created", "created", "date"));
    }

    private MetadataFieldMappingConfig mapping(
            final String aemField,
            final String searchStaxField,
            final String type) {

        final MetadataFieldMappingConfig config = new MetadataFieldMappingConfig();
        config.setAemField(aemField);
        config.setSearchStaxField(searchStaxField);
        config.setType(type);
        config.setEnabled(true);
        return config;
    }
}
