package com.searchstax.aem.connector.core.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolrFieldNameResolverTest {

    @Test
    void resolvesLocalizedTextFieldWithLanguage() {
        assertEquals("title_txt_en", SolrFieldNameResolver.resolve("title", "text", "en"));
    }

    @Test
    void resolvesLocalizedStringsFieldWithLanguage() {
        assertEquals("tags_ss_en_us", SolrFieldNameResolver.resolve("tags", "strings", "en_US"));
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
    void identifiesLocalizedTypes() {
        assertTrue(SolrFieldNameResolver.isLocalizedType("text"));
        assertTrue(SolrFieldNameResolver.isLocalizedType("strings"));
        assertFalse(SolrFieldNameResolver.isLocalizedType("boolean"));
    }
}
