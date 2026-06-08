package com.searchstax.aem.connector.core.incremental;

import org.junit.jupiter.api.Test;
import org.osgi.service.event.Event;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexingEventPathsTest {

    @Test
    void readsPathsFromDistributionProperty() {
        final Event event = new Event(
                "org/apache/sling/distribution/agent/package/distributed",
                Map.of("distribution.paths", new String[] {"/content/wknd/en/page"}));

        assertArrayEquals(
                new String[] {"/content/wknd/en/page"},
                IndexingEventPaths.readPaths(event));
    }

    @Test
    void readsSinglePathProperty() {
        final Event event = new Event("com/day/cq/replication/action", Map.of("path", "/content/site/page"));

        assertArrayEquals(new String[] {"/content/site/page"}, IndexingEventPaths.readPaths(event));
    }

    @Test
    void mapsActivateToIndexAndDeactivateToDelete() {
        assertEquals(IndexingAction.INDEX, IndexingEventPaths.mapEventType("ACTIVATE"));
        assertEquals(IndexingAction.DELETE, IndexingEventPaths.mapEventType("DEACTIVATE"));
        assertNull(IndexingEventPaths.mapEventType("TEST"));
    }

    @Test
    void deduplicatesPathsPreservingOrder() {
        final List<String> deduped = IndexingEventPaths.dedupePaths(
                new String[] {"/content/a", "/content/b", "/content/a", "  "});

        assertEquals(Arrays.asList("/content/a", "/content/b"), deduped);
    }

    @Test
    void readsReplicationTypeFromAlternatePropertyNames() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("replicationType", "DELETE");
        final Event event = new Event("com/day/cq/replication/job", properties);

        assertEquals("DELETE", IndexingEventPaths.readType(event));
    }

    @Test
    void returnsNullTypeWhenNoTypePropertiesPresent() {
        assertNull(IndexingEventPaths.readType(new Event("topic", Collections.emptyMap())));
    }

    @Test
    void returnsEmptyPathsForNullEvent() {
        assertArrayEquals(new String[0], IndexingEventPaths.readPaths(null));
        assertNull(IndexingEventPaths.readType(null));
    }

    @Test
    void readsPathsFromCollectionProperty() {
        final Event event = new Event(
                "org/apache/sling/distribution/agent/package/distributed",
                Map.of("paths", Arrays.asList("/content/a", "/content/b")));

        assertArrayEquals(new String[] {"/content/a", "/content/b"}, IndexingEventPaths.readPaths(event));
    }

    @Test
    void readsPathsFromObjectArrayProperty() {
        final Event event = new Event(
                "com/day/cq/replication/action",
                Map.of("distribution.deep.paths", new Object[] {"/content/deep"}));

        assertArrayEquals(new String[] {"/content/deep"}, IndexingEventPaths.readPaths(event));
    }

    @Test
    void mapsAddAndDeleteAliases() {
        assertEquals(IndexingAction.INDEX, IndexingEventPaths.mapEventType("ADD"));
        assertEquals(IndexingAction.DELETE, IndexingEventPaths.mapEventType("DELETE"));
    }

    @Test
    void dedupePathsReturnsEmptyForNullOrBlankInput() {
        assertTrue(IndexingEventPaths.dedupePaths(null).isEmpty());
        assertTrue(IndexingEventPaths.dedupePaths(new String[] {" ", ""}).isEmpty());
    }

    @Test
    void ignoresBlankPathValuesInStringArray() {
        final Event event = new Event(
                "com/day/cq/replication/action",
                Map.of("paths", new String[] {" /content/a ", null, ""}));

        assertArrayEquals(new String[] {"/content/a"}, IndexingEventPaths.readPaths(event));
    }

    @Test
    void returnsEmptyPathsWhenAllPathValuesBlank() {
        final Event event = new Event(
                "com/day/cq/replication/action",
                Map.of("paths", new String[] {" ", ""}));

        assertArrayEquals(new String[0], IndexingEventPaths.readPaths(event));
    }

    @Test
    void readsTypeFromActionProperty() {
        final Event event = new Event("topic", Collections.singletonMap("action", " activate "));
        assertEquals("activate", IndexingEventPaths.readType(event));
    }

    @Test
    void mapEventTypeReturnsNullForNullType() {
        assertNull(IndexingEventPaths.mapEventType(null));
    }

    @Test
    void readPathsReturnsEmptyForUnsupportedPropertyType() {
        final Event event = new Event("topic", Map.of("path", 42));
        assertArrayEquals(new String[0], IndexingEventPaths.readPaths(event));
    }

    @Test
    void normalizePathsReturnsEmptyForNullArray() throws Exception {
        final var method = IndexingEventPaths.class.getDeclaredMethod("normalizePaths", String[].class);
        method.setAccessible(true);
        assertArrayEquals(new String[0], (String[]) method.invoke(null, (Object) null));
    }

    @Test
    void dedupePathsIgnoresNullElements() {
        final List<String> deduped = IndexingEventPaths.dedupePaths(
                new String[] {null, " ", "/content/a", "/content/a"});

        assertEquals(Collections.singletonList("/content/a"), deduped);
    }
}
