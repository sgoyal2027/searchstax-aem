package com.searchstax.aem.connector.core.utils;

import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;
import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;

import java.util.HashSet;
import java.util.Set;

public final class WizardMappingDuplicateValidator {

    private WizardMappingDuplicateValidator() {
    }

    public static String metadataMappingKey(final MetadataFieldMappingConfig config) {
        if (config == null) {
            return null;
        }
        if ("custom".equalsIgnoreCase(config.getAemField())) {
            final String customProperty = MultifieldParseHelper.trimToEmpty(config.getCustomProperty());
            return customProperty.isEmpty() ? null : "custom:" + customProperty;
        }
        final String aemField = MultifieldParseHelper.trimToEmpty(config.getAemField());
        return aemField.isEmpty() ? null : aemField;
    }

    public static String languageMappingKey(final LanguageMappingConfig config) {
        if (config == null) {
            return null;
        }
        final String aemLanguage = MultifieldParseHelper.trimToEmpty(config.getAemLanguage());
        return aemLanguage.isEmpty() ? null : aemLanguage;
    }

    public static String findDuplicateMetadataKey(final Iterable<MetadataFieldMappingConfig> mappings) {
        final Set<String> seen = new HashSet<>();
        for (final MetadataFieldMappingConfig mapping : mappings) {
            final String key = metadataMappingKey(mapping);
            if (key == null || key.isBlank()) {
                continue;
            }
            if (!seen.add(key)) {
                return key;
            }
        }
        return null;
    }

    public static String findDuplicateLanguageKey(final Iterable<LanguageMappingConfig> mappings) {
        final Set<String> seen = new HashSet<>();
        for (final LanguageMappingConfig mapping : mappings) {
            final String key = languageMappingKey(mapping);
            if (key == null || key.isBlank()) {
                continue;
            }
            if (!seen.add(key)) {
                return key;
            }
        }
        return null;
    }
}
