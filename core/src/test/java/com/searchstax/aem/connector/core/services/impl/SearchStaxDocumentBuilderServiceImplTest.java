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
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    void returnsEmptyDocumentForBlankDocumentId() {
        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                " ");

        assertTrue(document.isEmpty());
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
    void supportsStringScalarForStringsMappedFields() {
        stubMetadataMappings();
        final Resource content = createPageContentWithTagsScalar("Arctic Surfing", "Northern Norway surf story");
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH))).thenReturn(content);
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.of("en"));

        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                PAGE_PATH);

        assertTrue(document.isPresent());
        assertTrue(document.get().get("tags_ss_en").isArray());
        assertEquals("wknd:activity/surfing", document.get().get("tags_ss_en").get(0).asText());
    }

    @Test
    void readsDateFromDateTypeWhenCalendarIsMissing() {
        stubMetadataMappings();
        final Resource content = createPageContentWithCreatedAsDate("Arctic Surfing", "Northern Norway surf story");
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH))).thenReturn(content);
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.of("en"));

        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                PAGE_PATH);

        assertTrue(document.isPresent());
        assertTrue(document.get().has("created_dt"));
    }

    @Test
    void returnsEmptyWhenDocumentExceedsSizeLimit() {
        stubMetadataMappings();
        final String hugeDescription = "x".repeat(200_000);
        final Resource content = createPageContent("Arctic Surfing", hugeDescription);
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH))).thenReturn(content);
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.of("en"));

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

    @Test
    void returnsEmptyDeletePayloadForBlankPath() {
        assertTrue(documentBuilderService.buildDeletePayload(" ").isEmpty());
    }

    @Test
    void returnsEmptyDocumentWhenContentResourceMissing() {
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH))).thenReturn(null);

        assertTrue(documentBuilderService.buildDocument(context.resourceResolver(), PAGE_PATH).isEmpty());
    }

    @Test
    void buildsDocumentWithCustomPropertyMapping() {
        final MetadataFieldMappingConfig customMapping = new MetadataFieldMappingConfig();
        customMapping.setAemField("custom");
        customMapping.setCustomProperty("customField");
        customMapping.setSearchStaxField("customField");
        customMapping.setType("text");
        customMapping.setEnabled(true);

        when(metadataFieldConfigService.getMetadataFieldMappings()).thenReturn(Arrays.asList(customMapping));
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH)))
                .thenReturn(createPageContent("Title", "Description", "custom value"));
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.of("en"));

        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                PAGE_PATH);

        assertTrue(document.isPresent());
        assertEquals("custom value", document.get().get("customField_txt_en").asText());
    }

    @Test
    void buildsDocumentWithBooleanAndNumericFields() {
        final List<MetadataFieldMappingConfig> mappings = Arrays.asList(
                mapping("featured", "featured", "boolean"),
                mapping("priority", "priority", "int"),
                mapping("score", "score", "double"));

        when(metadataFieldConfigService.getMetadataFieldMappings()).thenReturn(mappings);
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH)))
                .thenReturn(createPageContentWithScalars(true, 5, 1.5d));
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.of("en"));

        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                PAGE_PATH);

        assertTrue(document.isPresent());
        assertTrue(document.get().get("featured_b").asBoolean());
        assertEquals(5, document.get().get("priority_i").asInt());
        assertEquals(1.5d, document.get().get("score_d").asDouble(), 0.001d);
    }

    @Test
    void skipsMappingsWithBlankPropertyOrSolrFieldNames() {
        final MetadataFieldMappingConfig blankProperty = mapping("jcr:title", "title", "text");
        blankProperty.setAemField(" ");
        final MetadataFieldMappingConfig blankSolr = mapping("jcr:description", " ", "text");

        when(metadataFieldConfigService.getMetadataFieldMappings())
                .thenReturn(Arrays.asList(blankProperty, blankSolr));
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH)))
                .thenReturn(createPageContent("Title", "Description"));
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.of("en"));

        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                PAGE_PATH);

        assertTrue(document.isPresent());
        assertFalse(document.get().has("title_txt_en"));
        assertFalse(document.get().has("description_txt_en"));
    }

    @Test
    void skipsMissingPropertyValues() {
        final MetadataFieldMappingConfig missingValue = mapping("missingField", "missing", "text");
        when(metadataFieldConfigService.getMetadataFieldMappings()).thenReturn(Collections.singletonList(missingValue));
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH)))
                .thenReturn(createPageContent("Title", "Description"));
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.of("en"));

        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                PAGE_PATH);

        assertTrue(document.isPresent());
        assertFalse(document.get().has("missing_txt_en"));
    }

    @Test
    void buildsDocumentUsingStringScalarForStringsFieldType() {
        final MetadataFieldMappingConfig stringsMapping = mapping("cq:tags", "tags", "strings");
        when(metadataFieldConfigService.getMetadataFieldMappings()).thenReturn(Collections.singletonList(stringsMapping));
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH)))
                .thenReturn(createPageContentWithTagsScalar("Title", "Description"));
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.of("en"));

        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                PAGE_PATH);

        assertTrue(document.isPresent());
        assertEquals("wknd:activity/surfing", document.get().get("tags_ss_en").get(0).asText());
    }

    @Test
    void buildsDocumentWithDatePropertyStoredAsJavaDate() {
        final MetadataFieldMappingConfig dateMapping = mapping("jcr:created", "created", "date");
        when(metadataFieldConfigService.getMetadataFieldMappings()).thenReturn(Collections.singletonList(dateMapping));

        context.create().resource(PAGE_PATH, "jcr:primaryType", "cq:Page");
        context.create().resource(
                PAGE_PATH + "/jcr:content",
                "jcr:primaryType",
                "cq:PageContent",
                "jcr:language",
                "en",
                "jcr:created",
                new Date());

        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH)))
                .thenReturn(context.resourceResolver().getResource(PAGE_PATH + "/jcr:content"));
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.of("en"));

        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                PAGE_PATH);

        assertTrue(document.isPresent());
        assertTrue(document.get().has("created_dt"));
    }

    @Test
    void buildsDocumentWithLongFloatAndDateWithoutCalendar() {
        final List<MetadataFieldMappingConfig> mappings = Arrays.asList(
                mapping("viewCount", "views", "long"),
                mapping("rating", "rating", "float"),
                mapping("published", "published", "date"));

        when(metadataFieldConfigService.getMetadataFieldMappings()).thenReturn(mappings);
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH)))
                .thenReturn(createPageContentWithExtendedScalars(42L, 4.5f, new Date()));
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.of("en"));

        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                PAGE_PATH);

        assertTrue(document.isPresent());
        assertEquals(42L, document.get().get("views_l").asLong());
        assertEquals(4.5f, document.get().get("rating_f").floatValue(), 0.01f);
        assertTrue(document.get().has("published_dt"));
    }

    @Test
    void skipsDisabledMetadataMappings() {
        final MetadataFieldMappingConfig disabled = mapping("jcr:title", "title", "text");
        disabled.setEnabled(false);

        when(metadataFieldConfigService.getMetadataFieldMappings()).thenReturn(Collections.singletonList(disabled));
        when(indexingScopeService.resolveIndexableResource(any(), eq(PAGE_PATH)))
                .thenReturn(createPageContent("Hidden", "Description"));
        when(languageConfigService.mapToSearchStaxLanguage("en")).thenReturn(Optional.of("en"));

        final Optional<ObjectNode> document = documentBuilderService.buildDocument(
                context.resourceResolver(),
                PAGE_PATH);

        assertTrue(document.isPresent());
        assertFalse(document.get().has("title_txt_en"));
    }

    private Resource createPageContent(final String title, final String description) {
        return createPageContent(title, description, null);
    }

    private Resource createPageContent(final String title, final String description, final String customField) {
        context.create().resource(
                PAGE_PATH,
                "jcr:primaryType",
                "cq:Page");
        final Map<String, Object> properties = new java.util.HashMap<>();
        properties.put("jcr:primaryType", "cq:PageContent");
        properties.put("jcr:language", "en");
        properties.put("jcr:title", title);
        properties.put("jcr:description", description);
        properties.put("cq:tags", new String[] {"wknd:activity/surfing"});
        properties.put("jcr:created", Calendar.getInstance());
        if (customField != null) {
            properties.put("customField", customField);
        }

        context.create().resource(PAGE_PATH + "/jcr:content", properties);
        return context.resourceResolver().getResource(PAGE_PATH + "/jcr:content");
    }

    private Resource createPageContentWithExtendedScalars(
            final long viewCount,
            final float rating,
            final Date published) {

        context.create().resource(PAGE_PATH, "jcr:primaryType", "cq:Page");
        context.create().resource(
                PAGE_PATH + "/jcr:content",
                "jcr:primaryType",
                "cq:PageContent",
                "jcr:language",
                "en",
                "viewCount",
                viewCount,
                "rating",
                rating,
                "published",
                published);
        return context.resourceResolver().getResource(PAGE_PATH + "/jcr:content");
    }

    private Resource createPageContentWithScalars(
            final boolean featured,
            final int priority,
            final double score) {

        context.create().resource(PAGE_PATH, "jcr:primaryType", "cq:Page");
        context.create().resource(
                PAGE_PATH + "/jcr:content",
                "jcr:primaryType",
                "cq:PageContent",
                "jcr:language",
                "en",
                "featured",
                featured,
                "priority",
                priority,
                "score",
                score);
        return context.resourceResolver().getResource(PAGE_PATH + "/jcr:content");
    }

    private Resource createPageContentWithTagsScalar(
            final String title,
            final String description) {
        context.create().resource(
                PAGE_PATH,
                "jcr:primaryType",
                "cq:Page");
        final Map<String, Object> properties = new java.util.HashMap<>();
        properties.put("jcr:primaryType", "cq:PageContent");
        properties.put("jcr:language", "en");
        properties.put("jcr:title", title);
        properties.put("jcr:description", description);
        properties.put("cq:tags", "wknd:activity/surfing");
        properties.put("jcr:created", Calendar.getInstance());

        context.create().resource(PAGE_PATH + "/jcr:content", properties);
        return context.resourceResolver().getResource(PAGE_PATH + "/jcr:content");
    }

    private Resource createPageContentWithCreatedAsDate(
            final String title,
            final String description) {
        context.create().resource(
                PAGE_PATH,
                "jcr:primaryType",
                "cq:Page");
        final Map<String, Object> properties = new java.util.HashMap<>();
        properties.put("jcr:primaryType", "cq:PageContent");
        properties.put("jcr:language", "en");
        properties.put("jcr:title", title);
        properties.put("jcr:description", description);
        properties.put("cq:tags", new String[] {"wknd:activity/surfing"});
        properties.put("jcr:created", new Date());

        context.create().resource(PAGE_PATH + "/jcr:content", properties);
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
