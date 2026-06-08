package com.searchstax.aem.connector.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageCodeNormalizerTest {

    @Test
    void normalizesHyphenatedLocale() {
        assertEquals("en_us", LanguageCodeNormalizer.normalize("en-US"));
    }

    @Test
    void normalizesUnderscoreLocale() {
        assertEquals("en_us", LanguageCodeNormalizer.normalize("en_US"));
    }

    @Test
    void extractsBaseLanguageFromLocale() {
        assertEquals("en", LanguageCodeNormalizer.baseLanguage("en_us"));
    }

    @Test
    void keepsBaseLanguageWhenAlreadyShort() {
        assertEquals("de", LanguageCodeNormalizer.baseLanguage("de"));
    }

    @Test
    void returnsEmptyForBlankLanguageCodes() {
        assertEquals("", LanguageCodeNormalizer.normalize(null));
        assertEquals("", LanguageCodeNormalizer.normalize("  "));
        assertEquals("", LanguageCodeNormalizer.baseLanguage(null));
    }
}
