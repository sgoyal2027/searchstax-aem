package com.searchstax.aem.connector.core.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchstax.aem.connector.core.config.LanguageConfigService;
import com.searchstax.aem.connector.core.config.MetadataFieldConfigService;
import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.constants.SearchStaxServiceLimits;
import com.searchstax.aem.connector.core.incremental.AemLanguageResolver;
import com.searchstax.aem.connector.core.services.SearchStaxApiErrorPolicy;
import com.searchstax.aem.connector.core.services.IndexingScopeService;
import com.searchstax.aem.connector.core.services.SearchStaxDocumentBuilderService;
import com.searchstax.aem.connector.core.services.SolrFieldNameResolver;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component(service = SearchStaxDocumentBuilderService.class)
public class SearchStaxDocumentBuilderServiceImpl implements SearchStaxDocumentBuilderService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchStaxDocumentBuilderServiceImpl.class);

    private static final String CUSTOM_MAPPING = "custom";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Reference
    private MetadataFieldConfigService metadataFieldConfigService;

    @Reference
    private LanguageConfigService languageConfigService;

    @Reference
    private IndexingScopeService indexingScopeService;

    @Override
    public String resolveDocumentId(final String path) {
        return SearchStaxFullIndexPathConfigurationServiceImpl.normalizePath(path, false);
    }

    @Override
    public Optional<ObjectNode> buildDocument(final ResourceResolver resolver, final String path) {
        final String documentId = resolveDocumentId(path);
        if (documentId.isEmpty()) {
            return Optional.empty();
        }

        final Resource contentResource = indexingScopeService.resolveIndexableResource(resolver, path);
        if (contentResource == null) {
            LOG.warn("{} Unable to resolve indexable resource for {}", IncrementalIndexingDefaults.LOG_PREFIX, path);
            return Optional.empty();
        }

        final String aemLanguage = AemLanguageResolver.resolve(
                resolver,
                path,
                contentResource.getValueMap());
        final Optional<String> mappedLanguage = languageConfigService.mapToSearchStaxLanguage(aemLanguage);
        if (mappedLanguage.isEmpty()) {
            LOG.warn(
                    "{} No enabled language mapping for AEM language '{}' at {}; Solr field suffixes must come from Language Mapping config",
                    IncrementalIndexingDefaults.LOG_PREFIX,
                    aemLanguage,
                    path);
            return Optional.empty();
        }

        final String searchStaxLanguage = mappedLanguage.get();

        final ObjectNode document = objectMapper.createObjectNode();
        document.put(IncrementalIndexingDefaults.DOCUMENT_ID_FIELD, documentId);
        document.put(IncrementalIndexingDefaults.LANGUAGE_FIELD, searchStaxLanguage);

        int fieldCount = 2;
        for (final MetadataFieldMappingConfig mapping : getEnabledMappings()) {
            final String propertyName = resolvePropertyName(mapping);
            if (propertyName == null || propertyName.isBlank()) {
                continue;
            }

            final String solrField = SolrFieldNameResolver.resolve(
                    mapping.getSearchStaxField(),
                    mapping.getType(),
                    searchStaxLanguage);

            if (solrField == null || solrField.isBlank()) {
                continue;
            }

            final Object value = readPropertyValue(contentResource, propertyName, mapping.getType());
            if (value == null) {
                continue;
            }

            writeFieldValue(document, solrField, value, mapping.getType());
            fieldCount++;
        }

        final int documentBytes = document.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (documentBytes > SearchStaxServiceLimits.MAX_DOCUMENT_BYTES) {
            LOG.warn("{} Document for {} exceeds SearchStax 100 KB limit ({} bytes): {}",
                    IncrementalIndexingDefaults.LOG_PREFIX,
                    documentId,
                    documentBytes,
                    SearchStaxApiErrorPolicy.resolveGuidanceMessage(413, null));
            return Optional.empty();
        }

        LOG.info("{} Built document for {} with {} fields, aemLanguage={}, searchStaxLanguage={}, bytes={}",
                IncrementalIndexingDefaults.LOG_PREFIX,
                documentId,
                fieldCount,
                aemLanguage,
                searchStaxLanguage,
                documentBytes);
        return Optional.of(document);
    }

    @Override
    public Optional<ObjectNode> buildDeletePayload(final String path) {
        final String documentId = resolveDocumentId(path);
        if (documentId.isEmpty()) {
            return Optional.empty();
        }
        final ObjectNode deleteNode = objectMapper.createObjectNode();
        deleteNode.put(IncrementalIndexingDefaults.DOCUMENT_ID_FIELD, documentId);
        return Optional.of(deleteNode);
    }

    private List<MetadataFieldMappingConfig> getEnabledMappings() {
        return metadataFieldConfigService.getMetadataFieldMappings().stream()
                .filter(mapping -> mapping != null && mapping.isEnabled())
                .collect(Collectors.toList());
    }

    private String resolvePropertyName(final MetadataFieldMappingConfig mapping) {
        if (CUSTOM_MAPPING.equalsIgnoreCase(mapping.getAemField())) {
            return mapping.getCustomProperty();
        }
        return mapping.getAemField();
    }

    private Object readPropertyValue(
            final Resource resource,
            final String propertyName,
            final String fieldType) {

        final ValueMap properties = resource.getValueMap();
        if (!properties.containsKey(propertyName)) {
            return null;
        }

        if ("strings".equalsIgnoreCase(fieldType)) {
            final String[] values = properties.get(propertyName, String[].class);
            if (values != null && values.length > 0) {
                return values;
            }
            final String single = properties.get(propertyName, String.class);
            return single != null ? new String[] {single} : null;
        }

        if ("boolean".equalsIgnoreCase(fieldType)) {
            return properties.get(propertyName, false);
        }

        if ("int".equalsIgnoreCase(fieldType)) {
            return properties.get(propertyName, 0);
        }

        if ("long".equalsIgnoreCase(fieldType)) {
            return properties.get(propertyName, 0L);
        }

        if ("double".equalsIgnoreCase(fieldType)) {
            return properties.get(propertyName, 0.0d);
        }

        if ("float".equalsIgnoreCase(fieldType)) {
            return properties.get(propertyName, 0.0f);
        }

        if ("date".equalsIgnoreCase(fieldType)) {
            final Calendar calendar = properties.get(propertyName, Calendar.class);
            if (calendar != null) {
                return calendar.getTimeInMillis();
            }
            final Date date = properties.get(propertyName, Date.class);
            if (date != null) {
                return date.getTime();
            }
        }

        return properties.get(propertyName, String.class);
    }

    private void writeFieldValue(
            final ObjectNode document,
            final String fieldName,
            final Object value,
            final String fieldType) {

        if (value instanceof String[]) {
            final ArrayNode arrayNode = objectMapper.createArrayNode();
            for (final String item : (String[]) value) {
                if (item != null) {
                    arrayNode.add(item);
                }
            }
            document.set(fieldName, arrayNode);
            return;
        }

        if (value instanceof Boolean) {
            document.put(fieldName, (Boolean) value);
            return;
        }

        if (value instanceof Integer) {
            document.put(fieldName, (Integer) value);
            return;
        }

        if (value instanceof Long) {
            document.put(fieldName, (Long) value);
            return;
        }

        if (value instanceof Double) {
            document.put(fieldName, (Double) value);
            return;
        }

        if (value instanceof Float) {
            document.put(fieldName, (Float) value);
            return;
        }

        document.put(fieldName, String.valueOf(value));
    }
}
