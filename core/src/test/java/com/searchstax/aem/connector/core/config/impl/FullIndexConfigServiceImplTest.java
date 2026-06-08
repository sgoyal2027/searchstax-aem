package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.model.FullIndexConfig;
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class FullIndexConfigServiceImplTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    private FullIndexConfigServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
        service = new FullIndexConfigServiceImpl();
        TestReflection.inject(service, "resolverUtil", resolverUtil);
    }

    @Test
    void returnsEmptyConfigWhenResourceMissing() {
        final FullIndexConfig config = service.getConfiguration();

        assertNull(config.getIncludePaths());
    }

    @Test
    void loadsRootPathIncludePathsAndExcludePaths() {
        context.create().resource(
                "/conf/searchstaxconnector/settings/fullindexsetupconfig",
                "rootPath", "/content/wknd",
                "excludePaths", new String[] {"/content/wknd/private"});

        context.create().resource(
                "/conf/searchstaxconnector/settings/fullindexsetupconfig/includePaths/item0",
                "path", "/content/wknd/us/en",
                "includeChildPath", true);

        final FullIndexConfig config = service.getConfiguration();

        assertEquals("/content/wknd", config.getRootPath());
        assertEquals(1, config.getIncludePaths().size());
        assertEquals("/content/wknd/us/en", config.getIncludePaths().get(0).getPath());
        assertTrue(config.getIncludePaths().get(0).isIncludeChildPath());
        assertArrayEquals(new String[] {"/content/wknd/private"}, config.getExcludePaths());
    }
}
