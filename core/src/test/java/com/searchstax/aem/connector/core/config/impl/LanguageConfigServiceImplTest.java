package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageConfigServiceImplTest {

    @Test
    void mapsExactAemLanguageToConfiguredSearchStaxSuffix() {
        final LanguageConfigServiceImpl service = new LanguageConfigServiceImpl() {
            @Override
            public java.util.List<LanguageMappingConfig> getLanguageMappings() {
                return Arrays.asList(mapping("en", "abc", true));
            }
        };

        assertEquals(Optional.of("abc"), service.mapToSearchStaxLanguage("en"));
    }

    @Test
    void mapsBaseLanguageWhenOnlyLocaleConfigured() {
        final LanguageConfigServiceImpl service = new LanguageConfigServiceImpl() {
            @Override
            public java.util.List<LanguageMappingConfig> getLanguageMappings() {
                return Arrays.asList(mapping("en_US", "en", true));
            }
        };

        assertEquals(Optional.of("en"), service.mapToSearchStaxLanguage("en"));
    }

    @Test
    void prefersExactLocaleMatchOverBaseLanguageMatch() {
        final LanguageConfigServiceImpl service = new LanguageConfigServiceImpl() {
            @Override
            public java.util.List<LanguageMappingConfig> getLanguageMappings() {
                return Arrays.asList(
                        mapping("en", "generic_en", true),
                        mapping("en_US", "us_en", true));
            }
        };

        assertEquals(Optional.of("us_en"), service.mapToSearchStaxLanguage("en_US"));
    }

    @Test
    void ignoresDisabledMappings() {
        final LanguageConfigServiceImpl service = new LanguageConfigServiceImpl() {
            @Override
            public java.util.List<LanguageMappingConfig> getLanguageMappings() {
                return Arrays.asList(mapping("en", "abc", false));
            }
        };

        assertTrue(service.mapToSearchStaxLanguage("en").isEmpty());
    }

    private static LanguageMappingConfig mapping(
            final String aemLanguage,
            final String searchStaxLanguage,
            final boolean enabled) {

        final LanguageMappingConfig config = new LanguageMappingConfig();
        config.setAemLanguage(aemLanguage);
        config.setSearchStaxLanguage(searchStaxLanguage);
        config.setEnabled(enabled);
        return config;
    }
}
