package com.searchstax.aem.connector.core.utils;

import java.util.Locale;

public final class LanguageCodeNormalizer {

    private LanguageCodeNormalizer() {
    }

    public static String normalize(final String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return "";
        }
        return languageCode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    public static String baseLanguage(final String languageCode) {
        final String normalized = normalize(languageCode);
        if (normalized.isEmpty()) {
            return "";
        }

        final int separator = normalized.indexOf('_');
        if (separator > 0) {
            return normalized.substring(0, separator);
        }

        return normalized;
    }
}
