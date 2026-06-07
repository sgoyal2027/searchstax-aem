package com.searchstax.aem.connector.core.services;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class SolrFieldNameResolver {

    private static final Set<String> NON_LOCALIZED_TYPES = new HashSet<>();

    static {
        NON_LOCALIZED_TYPES.add("boolean");
        NON_LOCALIZED_TYPES.add("int");
        NON_LOCALIZED_TYPES.add("long");
        NON_LOCALIZED_TYPES.add("double");
        NON_LOCALIZED_TYPES.add("float");
        NON_LOCALIZED_TYPES.add("date");
    }

    private SolrFieldNameResolver() {
    }

    public static String resolve(final String searchStaxField, final String type, final String language) {
        if (searchStaxField == null || searchStaxField.isBlank()) {
            return null;
        }

        final String normalizedType = type == null ? "text" : type.trim().toLowerCase(Locale.ROOT);
        final String suffix = suffixForType(normalizedType);
        final String baseName = searchStaxField.trim() + suffix;

        if (isLocalizedType(normalizedType)) {
            final String normalizedLanguage = normalizeLanguage(language);
            if (normalizedLanguage.isEmpty()) {
                return baseName;
            }
            return baseName + "_" + normalizedLanguage;
        }

        return baseName;
    }

    public static boolean isLocalizedType(final String type) {
        if (type == null || type.isBlank()) {
            return true;
        }
        return !NON_LOCALIZED_TYPES.contains(type.trim().toLowerCase(Locale.ROOT));
    }

    public static String suffixForType(final String type) {
        if (type == null || type.isBlank()) {
            return "_txt";
        }

        switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "text":
                return "_txt";
            case "strings":
                return "_ss";
            case "string":
                return "_s";
            case "boolean":
                return "_b";
            case "int":
                return "_i";
            case "long":
                return "_l";
            case "double":
                return "_d";
            case "float":
                return "_f";
            case "date":
                return "_dt";
            default:
                return "_txt";
        }
    }

    public static String normalizeLanguage(final String language) {
        if (language == null || language.isBlank()) {
            return "";
        }
        return language.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
