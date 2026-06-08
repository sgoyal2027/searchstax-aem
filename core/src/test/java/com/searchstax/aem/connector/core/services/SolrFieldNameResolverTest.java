package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolrFieldNameResolverTest {
    @Test
    void resolvesNullForNullOrBlankField() {
        assertEquals(null, SolrFieldNameResolver.resolve(null, "text", "en"));
        assertEquals(null, SolrFieldNameResolver.resolve(" ", "text", "en"));
    }

    @Test
    void resolvesLocalizedTextFieldWithLanguage() {
        assertEquals("title_txt_en", SolrFieldNameResolver.resolve("title", "text", "en"));
    }

    @Test
    void resolvesLocalizedStringsFieldWithLanguage() {
        assertEquals("tags_ss_en_us", SolrFieldNameResolver.resolve("tags", "strings", "en_US"));
    }

    @Test
    void normalizesLanguageInHyphenatedLocale() {
        assertEquals("tags_ss_en_us", SolrFieldNameResolver.resolve("tags", "strings", "en-US"));
    }

    @Test
    void defaultsToTextTypeWhenTypeIsNull() {
        assertEquals("title_txt_en", SolrFieldNameResolver.resolve("title", null, "en"));
    }

    @Test
    void omitsLanguageSuffixWhenLanguageBlank() {
        assertEquals("title_txt", SolrFieldNameResolver.resolve("title", "text", " "));
    }

    @Test
    void resolvesWithBlankTypeAsText() {
        assertEquals("title_txt_en", SolrFieldNameResolver.resolve("title", "  ", "en"));
    }

    @Test
    void omitsLanguageForBooleanField() {
        assertEquals("featured_b", SolrFieldNameResolver.resolve("featured", "boolean", "en"));
    }

    @Test
    void omitsLanguageForNumericAndDateFields() {
        assertEquals("priority_i", SolrFieldNameResolver.resolve("priority", "int", "en"));
        assertEquals("count_l", SolrFieldNameResolver.resolve("count", "long", "de"));
        assertEquals("score_d", SolrFieldNameResolver.resolve("score", "double", "en"));
        assertEquals("rating_f", SolrFieldNameResolver.resolve("rating", "float", "en"));
        assertEquals("published_dt", SolrFieldNameResolver.resolve("published", "date", "en"));
    }

    @Test
    void usesSingularStringSuffixForStringType() {
        assertEquals("tag_s_en", SolrFieldNameResolver.resolve("tag", "string", "en"));
    }

    @Test
    void identifiesLocalizedTypes() {
        assertTrue(SolrFieldNameResolver.isLocalizedType("text"));
        assertTrue(SolrFieldNameResolver.isLocalizedType("strings"));
        assertFalse(SolrFieldNameResolver.isLocalizedType("boolean"));
    }

    @Test
    void considersNullOrBlankTypeLocalized() {
        assertTrue(SolrFieldNameResolver.isLocalizedType(null));
        assertTrue(SolrFieldNameResolver.isLocalizedType(" "));
        assertTrue(SolrFieldNameResolver.isLocalizedType(""));
    }

    @Test
    void fallsBackToTextSuffixForUnknownTypes() {
        assertEquals("_txt", SolrFieldNameResolver.suffixForType("not-a-type"));
    }

    @Test
    void suffixForTypeReturnsTextForNullOrBlank() {
        assertEquals("_txt", SolrFieldNameResolver.suffixForType(null));
        assertEquals("_txt", SolrFieldNameResolver.suffixForType(" "));
        assertEquals("_txt", SolrFieldNameResolver.suffixForType(""));
    }

    @Test
    void normalizeLanguageToEmptyForNullOrBlank() {
        assertEquals("", SolrFieldNameResolver.normalizeLanguage(null));
        assertEquals("", SolrFieldNameResolver.normalizeLanguage(" "));
    }
}
