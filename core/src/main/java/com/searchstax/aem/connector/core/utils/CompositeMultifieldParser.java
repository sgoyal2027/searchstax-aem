package com.searchstax.aem.connector.core.utils;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CompositeMultifieldParser {

    private CompositeMultifieldParser() {
    }

    public static Map<Integer, Map<String, String>> parse(
            final SlingHttpServletRequest request,
            final String multifieldName) {

        final Map<Integer, Map<String, String>> items = new TreeMap<>();

        final Pattern pattern = Pattern.compile(
                ".*?" + Pattern.quote(multifieldName) + "/item(\\d+)/(?:\\./)?([^/@]+)(?:@[^/]+)?$");

        for (final Map.Entry<String, RequestParameter[]> entry
                : request.getRequestParameterMap().entrySet()) {

            final String key = entry.getKey() == null ? "" : entry.getKey().trim();
            final Matcher matcher = pattern.matcher(key);
            if (!matcher.matches()) {
                continue;
            }

            final RequestParameter[] values = entry.getValue();
            if (values == null || values.length == 0) {
                continue;
            }

            final String value = resolveParameterValue(values);
            if (value == null) {
                continue;
            }

            final int index = Integer.parseInt(matcher.group(1));
            final String property = matcher.group(2);

            items.computeIfAbsent(index, itemKey -> new HashMap<>()).put(property, value);
        }

        return items;
    }

    public static String getValue(final Map<String, String> item, final String property) {
        if (item == null || property == null) {
            return null;
        }
        return item.get(property);
    }

    public static boolean isChecked(final Map<String, String> item, final String property) {
        final String value = getValue(item, property);
        final String normalized = value == null ? "" : value.trim();
        return "true".equalsIgnoreCase(normalized) || "on".equalsIgnoreCase(normalized);
    }

    private static String resolveParameterValue(final RequestParameter[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        if (values.length == 1) {
            return values[0].getString();
        }

        boolean anyChecked = false;
        boolean anyUnchecked = false;
        String lastValue = null;

        for (final RequestParameter parameter : values) {
            if (parameter == null) {
                continue;
            }
            final String value = parameter.getString();
            if (value == null) {
                continue;
            }
            lastValue = value;
            final String normalized = value.trim();
            if ("true".equalsIgnoreCase(normalized) || "on".equalsIgnoreCase(normalized)) {
                anyChecked = true;
            }
            if ("false".equalsIgnoreCase(normalized)) {
                anyUnchecked = true;
            }
        }

        if (anyChecked) {
            return "true";
        }
        if (anyUnchecked) {
            return "false";
        }
        return lastValue;
    }
}
