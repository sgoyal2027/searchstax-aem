package com.searchstax.aem.connector.core.utils;

import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;
import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WizardMappingDuplicateValidatorTest {

    @Test
    void detectsDuplicatePresetMetadataMapping() {
        final MetadataFieldMappingConfig first = new MetadataFieldMappingConfig();
        first.setAemField("jcr:title");

        final MetadataFieldMappingConfig second = new MetadataFieldMappingConfig();
        second.setAemField("jcr:title");

        assertEquals("jcr:title", WizardMappingDuplicateValidator.findDuplicateMetadataKey(
                List.of(first, second)));
    }

    @Test
    void detectsDuplicateCustomMetadataMapping() {
        final MetadataFieldMappingConfig first = new MetadataFieldMappingConfig();
        first.setAemField("custom");
        first.setCustomProperty("myproject:seoTitle");

        final MetadataFieldMappingConfig second = new MetadataFieldMappingConfig();
        second.setAemField("custom");
        second.setCustomProperty("myproject:seoTitle");

        assertEquals("custom:myproject:seoTitle", WizardMappingDuplicateValidator.findDuplicateMetadataKey(
                List.of(first, second)));
    }

    @Test
    void allowsDistinctCustomMetadataMappings() {
        final MetadataFieldMappingConfig first = new MetadataFieldMappingConfig();
        first.setAemField("custom");
        first.setCustomProperty("myproject:seoTitle");

        final MetadataFieldMappingConfig second = new MetadataFieldMappingConfig();
        second.setAemField("custom");
        second.setCustomProperty("myproject:keywords");

        assertNull(WizardMappingDuplicateValidator.findDuplicateMetadataKey(List.of(first, second)));
    }

    @Test
    void detectsDuplicateLanguageMapping() {
        final LanguageMappingConfig first = new LanguageMappingConfig();
        first.setAemLanguage("en_US");

        final LanguageMappingConfig second = new LanguageMappingConfig();
        second.setAemLanguage("en_US");

        assertEquals("en_US", WizardMappingDuplicateValidator.findDuplicateLanguageKey(
                List.of(first, second)));
    }
}
