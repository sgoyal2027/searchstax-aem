package com.searchstax.aem.connector.core.utils;

import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(AemContextExtension.class)
class ConfigResourceUtilTest {

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.create().resource("/conf/searchstaxconnector/settings");
    }

    @Test
    void returnsExistingResourceWhenPresent() throws PersistenceException {
        context.create().resource("/conf/searchstaxconnector/settings/apiconfig");

        final Resource resource = ConfigResourceUtil.getOrCreateConfigResource(
                context.resourceResolver(),
                "/conf/searchstaxconnector/settings/apiconfig");

        assertNotNull(resource);
        assertEquals("/conf/searchstaxconnector/settings/apiconfig", resource.getPath());
    }

    @Test
    void createsMissingConfigResourceUnderExistingParent() throws PersistenceException {
        final Resource resource = ConfigResourceUtil.getOrCreateConfigResource(
                context.resourceResolver(),
                "/conf/searchstaxconnector/settings/apiconfig");

        assertNotNull(resource);
        assertNotNull(context.resourceResolver().getResource("/conf/searchstaxconnector/settings/apiconfig"));
    }

    @Test
    void returnsNullWhenConfigPathHasNoParentSegment() throws PersistenceException {
        assertNull(ConfigResourceUtil.getOrCreateConfigResource(
                context.resourceResolver(),
                "apiconfig"));
    }

    @Test
    void returnsNullWhenParentDoesNotExist() throws PersistenceException {
        assertNull(ConfigResourceUtil.getOrCreateConfigResource(
                context.resourceResolver(),
                "/conf/missing/settings/apiconfig"));
    }

    @Test
    void getModifiablePropertiesReturnsValueMapForResource() {
        context.create().resource("/conf/searchstaxconnector/settings/apiconfig", "endpointUrl", "https://example.com");

        final Resource resource = context.resourceResolver().getResource("/conf/searchstaxconnector/settings/apiconfig");
        final ModifiableValueMap properties = ConfigResourceUtil.getModifiableProperties(resource);

        assertNotNull(properties);
        assertEquals("https://example.com", properties.get("endpointUrl", String.class));
    }

    @Test
    void getModifiablePropertiesReturnsNullForMissingResource() {
        assertNull(ConfigResourceUtil.getModifiableProperties(null));
    }
}
