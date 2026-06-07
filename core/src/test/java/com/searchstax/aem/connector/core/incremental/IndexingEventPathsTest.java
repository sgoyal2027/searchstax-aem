package com.searchstax.aem.connector.core.incremental;

import org.junit.jupiter.api.Test;
import org.osgi.service.event.Event;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
