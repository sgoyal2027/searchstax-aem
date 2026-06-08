package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.services.FullIndexPathConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchStaxFullIndexPathConfigurationServiceImplTest {

    private final SearchStaxFullIndexPathConfigurationServiceImpl service =
            new SearchStaxFullIndexPathConfigurationServiceImpl();

    @Test
    void normalizesPathsAndTrimsTrailingSlashForNonRootPaths() {
        assertEqualsNormalized("/content/site/page", "content/site/page/", false);
        assertEqualsNormalized("/content/site/", "/content/site/", true);
    }

    @Test
    void resolvesIncludesUnderRootOnly() {
        final FullIndexPathConfig config = new FullIndexPathConfig(
                "/content/wknd",
                new String[] {"/content/wknd/en", "/content/other"},
                new boolean[] {true, true},
                new String[0]);

        assertArrayEquals(
                new String[] {"/content/wknd/en"},
                service.resolveEffectiveIncludes(config));
    }

    @Test
    void detectsExcludedPaths() {
        assertTrue(SearchStaxFullIndexPathConfigurationServiceImpl.isPathUnder(
                "/content/wknd/en/page",
                "/content/wknd/en"));
        assertFalse(SearchStaxFullIndexPathConfigurationServiceImpl.isPathUnder(
                "/content/other/page",
                "/content/wknd"));
        assertTrue(service.isExcludedPath(
                "/content/wknd/private/page",
                new String[] {"/content/wknd/private"}));
    }

    @Test
    void returnsEmptyIncludesWhenConfigMissing() {
        assertArrayEquals(new String[0], service.resolveEffectiveIncludes(null));
        assertArrayEquals(new String[0], service.resolveEffectiveExcludes(null));
    }

    @Test
    void returnsEmptyIncludesWhenRootPathMissing() {
        final FullIndexPathConfig config = new FullIndexPathConfig("", new String[0], new boolean[0], new String[0]);

        assertArrayEquals(new String[0], service.resolveEffectiveIncludes(config));
    }

    @Test
    void resolvesEffectiveExcludesUnderRoot() {
        final FullIndexPathConfig config = new FullIndexPathConfig(
                "/content/wknd",
                new String[0],
                new boolean[0],
                new String[] {"/content/wknd/private", "/content/other"});

        assertArrayEquals(
                new String[] {"/content/wknd/private"},
                service.resolveEffectiveExcludes(config));
    }

    @Test
    void deduplicatesEquivalentPaths() {
        assertArrayEquals(
                new String[] {"/content/a", "/content/b"},
                SearchStaxFullIndexPathConfigurationServiceImpl.normalizeAndDedupe(
                        new String[] {"/content/a", "content/a", "/content/b/"},
                        false));
    }

    @Test
    void normalizePathReturnsEmptyForNullAndInvalidPaths() {
        assertEquals("", SearchStaxFullIndexPathConfigurationServiceImpl.normalizePath(null, false));
        assertEquals("", SearchStaxFullIndexPathConfigurationServiceImpl.normalizePath("//bad", false));
    }

    @Test
    void isPathUnderReturnsFalseForNullOrEmptyInputs() {
        assertFalse(SearchStaxFullIndexPathConfigurationServiceImpl.isPathUnder(null, "/content"));
        assertFalse(SearchStaxFullIndexPathConfigurationServiceImpl.isPathUnder("/content", null));
    }

    @Test
    void isExcludedPathReturnsFalseForMissingInputs() {
        assertFalse(service.isExcludedPath(null, new String[] {"/content/x"}));
        assertFalse(service.isExcludedPath("/content/x", null));
        assertFalse(service.isExcludedPath("/content/x", new String[0]));
    }

    @Test
    void resolveEffectiveExcludesReturnsEmptyWhenRootMissing() {
        final FullIndexPathConfig config = new FullIndexPathConfig(
                " ",
                new String[0],
                new boolean[0],
                new String[] {"/content/x"});

        assertArrayEquals(new String[0], service.resolveEffectiveExcludes(config));
    }

    @Test
    void filterPathsUnderRootReturnsEmptyForMissingRootOrPaths() throws Exception {
        final var method = SearchStaxFullIndexPathConfigurationServiceImpl.class.getDeclaredMethod(
                "filterPathsUnderRoot",
                String.class,
                String[].class);
        method.setAccessible(true);

        assertArrayEquals(
                new String[0],
                (String[]) method.invoke(null, "", new String[] {"/content/a"}));
        assertArrayEquals(
                new String[0],
                (String[]) method.invoke(null, "/content", null));
    }

    private void assertEqualsNormalized(
            final String expected,
            final String input,
            final boolean rootPath) {

        assertEquals(expected, SearchStaxFullIndexPathConfigurationServiceImpl.normalizePath(input, rootPath));
    }

    private static void assertEquals(final String expected, final String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
