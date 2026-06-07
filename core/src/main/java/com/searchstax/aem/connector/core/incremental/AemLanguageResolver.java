package com.searchstax.aem.connector.core.incremental;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.searchstax.aem.connector.core.utils.LanguageCodeNormalizer;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import java.util.Locale;

/**
 * Resolves the AEM content language from page properties, PageManager, or path.
 */
public final class AemLanguageResolver {

    private static final String DEFAULT_LANGUAGE = "en";

    private AemLanguageResolver() {
    }

    public static String resolve(
            final ResourceResolver resolver,
            final String path,
            final ValueMap contentProperties) {

        final String fromProperty = readLanguageProperty(contentProperties);
        if (!fromProperty.isBlank()) {
            return fromProperty;
        }

        final String fromPage = readPageLanguage(resolver, path);
        if (!fromPage.isBlank()) {
            return fromPage;
        }

        final String fromPath = extractLanguageFromPath(path);
        if (!fromPath.isBlank()) {
            return fromPath;
        }

        return DEFAULT_LANGUAGE;
    }

    static String readLanguageProperty(final ValueMap contentProperties) {
        if (contentProperties == null) {
            return "";
        }

        final String jcrLanguage = contentProperties.get("jcr:language", String.class);
        if (jcrLanguage != null && !jcrLanguage.isBlank()) {
            return LanguageCodeNormalizer.normalize(jcrLanguage);
        }

        return "";
    }

    static String readPageLanguage(final ResourceResolver resolver, final String path) {
        if (resolver == null || path == null || path.isBlank()) {
            return "";
        }

        final PageManager pageManager = resolver.adaptTo(PageManager.class);
        if (pageManager == null) {
            return "";
        }

        final Page page = pageManager.getPage(path);
        if (page == null) {
            return "";
        }

        final Locale locale = page.getLanguage(false);
        if (locale == null) {
            return "";
        }

        final String language = locale.getLanguage();
        if (language == null || language.isBlank()) {
            return "";
        }

        final String country = locale.getCountry();
        if (country != null && !country.isBlank()) {
            return LanguageCodeNormalizer.normalize(language + "_" + country);
        }

        return LanguageCodeNormalizer.normalize(language);
    }

    static String extractLanguageFromPath(final String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        final String[] segments = path.split("/");
        int contentIndex = -1;
        for (int index = 0; index < segments.length; index++) {
            if ("content".equals(segments[index])) {
                contentIndex = index;
                break;
            }
        }

        if (contentIndex < 0) {
            return "";
        }

        for (int index = 0; index < segments.length - 1; index++) {
            if ("language-masters".equals(segments[index]) && isLocaleSegment(segments[index + 1])) {
                return LanguageCodeNormalizer.normalize(segments[index + 1]);
            }
        }

        final int rootIndex = contentIndex + 1;
        if (rootIndex >= segments.length) {
            return "";
        }

        if (rootIndex + 1 < segments.length && "dam".equals(segments[rootIndex])) {
            final int damSiteIndex = rootIndex + 1;
            if (damSiteIndex + 1 < segments.length && isLocaleSegment(segments[damSiteIndex + 1])) {
                return LanguageCodeNormalizer.normalize(segments[damSiteIndex + 1]);
            }
            return "";
        }

        if (rootIndex + 2 < segments.length
                && isLocaleSegment(segments[rootIndex + 1])
                && isLocaleSegment(segments[rootIndex + 2])) {
            return LanguageCodeNormalizer.normalize(segments[rootIndex + 2]);
        }

        if (rootIndex + 1 < segments.length && isLocaleSegment(segments[rootIndex + 1])) {
            return LanguageCodeNormalizer.normalize(segments[rootIndex + 1]);
        }

        return "";
    }

    static boolean isLocaleSegment(final String segment) {
        if (segment == null || segment.isBlank()) {
            return false;
        }

        return segment.length() == 2
                || segment.matches("[a-z]{2}_[A-Z]{2}")
                || segment.matches("[a-z]{2}-[A-Z]{2}");
    }

}
