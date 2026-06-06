package com.searchstax.aem.connector.core.utils;

import org.apache.sling.api.SlingHttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public final class MultifieldParseHelper {

    private static final int MAX_ITEMS = 200;

    private MultifieldParseHelper() {
    }

    public static Map<Integer, Map<String, String>> parseItems(
            final SlingHttpServletRequest request,
            final String multifieldName,
            final String... markerFields) {

        final Map<Integer, Map<String, String>> items =
                CompositeMultifieldParser.parse(request, multifieldName);

        if (!items.isEmpty()) {
            return items;
        }

        return parseLegacyItems(request, multifieldName, markerFields);
    }

    private static Map<Integer, Map<String, String>> parseLegacyItems(
            final SlingHttpServletRequest request,
            final String multifieldName,
            final String... markerFields) {

        final Map<Integer, Map<String, String>> items = new TreeMap<>();

        for (int index = 0; index < MAX_ITEMS; index++) {
            final String[] prefixes = {
                    "./" + multifieldName + "/item" + index + "/./",
                    "./" + multifieldName + "/item" + index + "/",
                    multifieldName + "/item" + index + "/"
            };

            String markerValue = null;
            for (final String markerField : markerFields) {
                markerValue = firstParameterValue(request, prefixes, markerField);
                if (markerValue != null) {
                    break;
                }
            }

            if (markerValue == null) {
                break;
            }

            final Map<String, String> item = new HashMap<>();
            item.put(markerFields[0], markerValue);

            for (final String fieldName : markerFields) {
                final String value = firstParameterValue(request, prefixes, fieldName);
                if (value != null) {
                    item.put(fieldName, value);
                }
            }

            items.put(index, item);
        }

        return items;
    }

    private static String firstParameterValue(
            final SlingHttpServletRequest request,
            final String[] prefixes,
            final String fieldName) {

        for (final String prefix : prefixes) {
            final String value = request.getParameter(prefix + fieldName);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    public static boolean isEnabled(final Map<String, String> item, final String property) {
        if (item == null || !item.containsKey(property)) {
            return true;
        }
        return CompositeMultifieldParser.isChecked(item, property);
    }

    public static String trimToEmpty(final String value) {
        return value == null ? "" : value.trim();
    }

    public static String[] getStringArrayParameter(
            final SlingHttpServletRequest request,
            final String fieldName) {

        String[] values = removeBlankValues(request.getParameterValues("./" + fieldName));
        if (values.length > 0) {
            return values;
        }

        values = removeBlankValues(request.getParameterValues(fieldName));
        if (values.length > 0) {
            return values;
        }

        final Map<Integer, Map<String, String>> items =
                parseItems(request, fieldName, fieldName);

        if (items.isEmpty()) {
            return new String[0];
        }

        return items.values().stream()
                .map(item -> item.get(fieldName))
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toArray(String[]::new);
    }

    private static String[] removeBlankValues(final String[] values) {
        if (values == null || values.length == 0) {
            return new String[0];
        }

        return java.util.Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toArray(String[]::new);
    }
}
