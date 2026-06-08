package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class MetadataFieldConfigServiceImplTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    private MetadataFieldConfigServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
        service = new MetadataFieldConfigServiceImpl();
        TestReflection.inject(service, "resolverUtil", resolverUtil);
    }

    @Test
    void returnsEmptyListWhenConfigurationMissing() {
        assertTrue(service.getMetadataFieldMappings().isEmpty());
    }

    @Test
    void parsesMetadataMappingsJsonFromConfiguration() {
        context.create().resource(
                "/conf/searchstaxconnector/settings/metadatafieldmapping",
                "metadataMappings",
                "[{\"aemField\":\"jcr:title\",\"searchStaxField\":\"title\",\"type\":\"text\",\"enabled\":true}]");

        final List<MetadataFieldMappingConfig> mappings = service.getMetadataFieldMappings();

        assertEquals(1, mappings.size());
        assertEquals("jcr:title", mappings.get(0).getAemField());
        assertEquals("title", mappings.get(0).getSearchStaxField());
        assertTrue(mappings.get(0).isEnabled());
    }

    @Test
    void returnsEmptyListWhenMappingsPropertyBlank() {
        context.create().resource(
                "/conf/searchstaxconnector/settings/metadatafieldmapping",
                "metadataMappings",
                " ");

        assertTrue(service.getMetadataFieldMappings().isEmpty());
    }

    @Test
    void returnsEmptyListForInvalidJson() {
        context.create().resource(
                "/conf/searchstaxconnector/settings/metadatafieldmapping",
                "metadataMappings",
                "not-json");

        assertTrue(service.getMetadataFieldMappings().isEmpty());
    }
}
