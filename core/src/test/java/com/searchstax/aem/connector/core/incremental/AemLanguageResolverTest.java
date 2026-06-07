package com.searchstax.aem.connector.core.incremental;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AemLanguageResolverTest {

    @Test
    void prefersJcrLanguageFromPageContent() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("jcr:language", "en");
        final ValueMap valueMap = new ValueMapDecorator(properties);

        assertEquals("en", AemLanguageResolver.readLanguageProperty(valueMap));
    }

    @Test
    void extractsLanguageFromCountryAndLocalePath() {
        assertEquals(
                "en",
                AemLanguageResolver.extractLanguageFromPath("/content/wknd/us/en/magazine/arctic-surfing"));
    }

    @Test
    void extractsLanguageFromSiteLocalePath() {
        assertEquals("de", AemLanguageResolver.extractLanguageFromPath("/content/mysite/de/products"));
    }

    @Test
    void extractsLanguageFromDamPath() {
        assertEquals(
                "en",
                AemLanguageResolver.extractLanguageFromPath("/content/dam/wknd-shared/en/magazine/article.jpg"));
    }

    @Test
    void extractsLanguageFromLanguageMastersPath() {
        assertEquals(
                "en",
                AemLanguageResolver.extractLanguageFromPath(
                        "/content/experience-fragments/wknd/language-masters/en/contributors/byline"));
    }
}
