package com.searchstax.aem.connector.core.incremental;

import org.osgi.service.event.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class IndexingEventPaths {

    private static final String[] PATH_PROPERTY_NAMES = {
            "distribution.paths",
            "distribution.deep.paths",
            "paths",
            "path"
    };

    private static final String[] TYPE_PROPERTY_NAMES = {
            "distribution.type",
            "type",
            "action",
            "replicationType"
    };

    private IndexingEventPaths() {
    }

    public static String[] readPaths(final Event event) {
        if (event == null) {
            return new String[0];
        }

        for (final String propertyName : PATH_PROPERTY_NAMES) {
            final String[] paths = toPathArray(event.getProperty(propertyName));
            if (paths.length > 0) {
                return paths;
            }
        }

        return new String[0];
    }

    public static String readType(final Event event) {
        if (event == null) {
            return null;
        }

        for (final String propertyName : TYPE_PROPERTY_NAMES) {
            final Object value = event.getProperty(propertyName);
            if (value != null) {
                final String type = String.valueOf(value).trim();
                if (!type.isEmpty()) {
                    return type;
                }
            }
        }

        return null;
    }

    public static IndexingAction mapEventType(final String type) {
        if (type == null) {
            return null;
        }

        switch (type.toUpperCase()) {
            case "ACTIVATE":
            case "ADD":
                return IndexingAction.INDEX;
            case "DEACTIVATE":
            case "DELETE":
                return IndexingAction.DELETE;
            default:
                return null;
        }
    }

    private static String[] toPathArray(final Object value) {
        if (value == null) {
            return new String[0];
        }

        if (value instanceof String[]) {
            return normalizePaths((String[]) value);
        }

        if (value instanceof String) {
            final String path = ((String) value).trim();
            return path.isEmpty() ? new String[0] : new String[] {path};
        }

        if (value instanceof Collection) {
            final List<String> paths = new ArrayList<>();
            for (final Object item : (Collection<?>) value) {
                if (item != null) {
                    final String path = String.valueOf(item).trim();
                    if (!path.isEmpty()) {
                        paths.add(path);
                    }
                }
            }
            return paths.toArray(new String[0]);
        }

        if (value.getClass().isArray()) {
            final Object[] items = (Object[]) value;
            final List<String> paths = new ArrayList<>();
            for (final Object item : items) {
                if (item != null) {
                    final String path = String.valueOf(item).trim();
                    if (!path.isEmpty()) {
                        paths.add(path);
                    }
                }
            }
            return paths.toArray(new String[0]);
        }

        return new String[0];
    }

    private static String[] normalizePaths(final String[] paths) {
        if (paths == null || paths.length == 0) {
            return new String[0];
        }

        final List<String> normalized = new ArrayList<>();
        for (final String path : paths) {
            if (path != null && !path.trim().isEmpty()) {
                normalized.add(path.trim());
            }
        }
        return normalized.toArray(new String[0]);
    }

    public static List<String> dedupePaths(final String[] paths) {
        if (paths == null || paths.length == 0) {
            return Collections.emptyList();
        }

        final List<String> deduped = new ArrayList<>();
        for (final String path : paths) {
            if (path != null && !path.isBlank() && !deduped.contains(path.trim())) {
                deduped.add(path.trim());
            }
        }
        return deduped;
    }
}
