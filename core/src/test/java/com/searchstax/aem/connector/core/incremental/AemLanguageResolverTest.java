package com.searchstax.aem.connector.core.incremental;

import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class AemLanguageResolverTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Test
    void prefersJcrLanguageFromPageContent() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("jcr:language", "en");
        final ValueMap valueMap = new ValueMapDecorator(properties);

        assertEquals("en", AemLanguageResolver.readLanguageProperty(valueMap));
    }

    @Test
    void normalizesJcrLanguageLocale() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("jcr:language", "en-US");
        final ValueMap valueMap = new ValueMapDecorator(properties);

        assertEquals("en_us", AemLanguageResolver.readLanguageProperty(valueMap));
    }

    @Test
    void defaultsToEnglishWhenLanguageCannotBeResolved() {
        assertEquals("en", AemLanguageResolver.resolve(null, "/content/unknown/path", null));
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

    @Test
    void returnsEmptyForNullContentProperties() {
        assertEquals("", AemLanguageResolver.readLanguageProperty(null));
    }

    @Test
    void returnsEmptyForBlankJcrLanguageProperty() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("jcr:language", " ");
        final ValueMap valueMap = new ValueMapDecorator(properties);

        assertEquals("", AemLanguageResolver.readLanguageProperty(valueMap));
    }

    @Test
    void readsPageLanguageFromPageManager() {
        context.create().page("/content/wknd/us/en/page");

        final String language = AemLanguageResolver.readPageLanguage(
                context.resourceResolver(),
                "/content/wknd/us/en/page");

        assertFalse(language.isBlank());
        assertTrue(language.startsWith("en"));
    }

    @Test
    void returnsEmptyWhenPageManagerIsNull() {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);

        assertEquals("", AemLanguageResolver.readPageLanguage(resolver, "/content/wknd/us/en/page"));
    }

    @Test
    void returnsEmptyWhenLocaleIsNull() {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        final PageManager pageManager = org.mockito.Mockito.mock(PageManager.class);
        final Page page = org.mockito.Mockito.mock(Page.class);

        when(resolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getPage("/content/wknd/us/en/page")).thenReturn(page);
        when(page.getLanguage(false)).thenReturn(null);

        assertEquals(
                "",
                AemLanguageResolver.readPageLanguage(resolver, "/content/wknd/us/en/page"));
    }

    @Test
    void resolvesLanguageFromPathWhenPropertyMissing() {
        assertEquals(
                "en",
                AemLanguageResolver.resolve(
                        context.resourceResolver(),
                        "/content/wknd/us/en/magazine/article",
                        null));
    }

    @Test
    void returnsEmptyForPathWithoutContentSegment() {
        assertEquals("", AemLanguageResolver.extractLanguageFromPath("/etc/designs/site"));
    }

    @Test
    void returnsEmptyForDamPathWithoutLocaleSegment() {
        assertEquals("", AemLanguageResolver.extractLanguageFromPath("/content/dam/wknd-shared/assets"));
    }

    @Test
    void extractsFirstLocaleSegmentWhenOnlySingleLocalePresent() {
        assertEquals(
                "us",
                AemLanguageResolver.extractLanguageFromPath("/content/wknd/us/products"));
    }

    @Test
    void returnsEmptyWhenLanguageMastersSegmentIsNotFollowedByLocale() {
        assertEquals(
                "",
                AemLanguageResolver.extractLanguageFromPath(
                        "/content/experience-fragments/wknd/language-masters/english/contributors"));
    }

    @Test
    void readPageLanguageNormalizesLanguageOnlyLocale() {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        final PageManager pageManager = org.mockito.Mockito.mock(PageManager.class);
        final Page page = org.mockito.Mockito.mock(Page.class);

        when(resolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getPage("/content/wknd/us/en/page")).thenReturn(page);
        when(page.getLanguage(false)).thenReturn(java.util.Locale.forLanguageTag("de"));

        assertEquals("de", AemLanguageResolver.readPageLanguage(resolver, "/content/wknd/us/en/page"));
    }

    @Test
    void readPageLanguageReturnsEmptyWhenLocaleLanguageBlank() {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        final PageManager pageManager = org.mockito.Mockito.mock(PageManager.class);
        final Page page = org.mockito.Mockito.mock(Page.class);

        when(resolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        when(pageManager.getPage("/content/wknd/us/en/page")).thenReturn(page);
        when(page.getLanguage(false)).thenReturn(new java.util.Locale("", "US"));

        assertEquals("", AemLanguageResolver.readPageLanguage(resolver, "/content/wknd/us/en/page"));
    }

    @Test
    void extractLanguageFromPathReturnsEmptyForBlankPath() {
        assertEquals("", AemLanguageResolver.extractLanguageFromPath(" "));
    }

    @Test
    void extractLanguageFromPathReturnsEmptyWhenContentHasNoFollowingSegment() {
        assertEquals("", AemLanguageResolver.extractLanguageFromPath("/content"));
    }

    @Test
    void validatesLocaleSegmentPatterns() {
        assertTrue(AemLanguageResolver.isLocaleSegment("en"));
        assertTrue(AemLanguageResolver.isLocaleSegment("en_US"));
        assertTrue(AemLanguageResolver.isLocaleSegment("en-US"));
        assertFalse(AemLanguageResolver.isLocaleSegment("english"));
        assertFalse(AemLanguageResolver.isLocaleSegment(null));
    }
}
