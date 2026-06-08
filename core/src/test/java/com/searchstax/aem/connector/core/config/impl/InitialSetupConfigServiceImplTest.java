package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class InitialSetupConfigServiceImplTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    private InitialSetupConfigServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
        service = new InitialSetupConfigServiceImpl();
        TestReflection.inject(service, "resolverUtil", resolverUtil);
    }

    @Test
    void returnsDefaultsWhenConfigurationMissing() {
        final InitialSetupConfig config = service.getConfiguration();

        assertFalse(config.isEnableConnector());
        assertNull(config.getRootPaths());
    }

    @Test
    void loadsInitialSetupPropertiesFromJcr() {
        context.create().resource(
                "/conf/searchstaxconnector/settings/initialsetupconfig",
                "enableConnector", true,
                "rootPaths", new String[] {"/content/wknd"},
                "excludePaths", new String[] {"/content/wknd/exclude"},
                "allowedFiles", new String[] {"pdf", "docx"});

        final InitialSetupConfig config = service.getConfiguration();

        assertTrue(config.isEnableConnector());
        assertArrayEquals(new String[] {"/content/wknd"}, config.getRootPaths());
        assertArrayEquals(new String[] {"/content/wknd/exclude"}, config.getExcludePaths());
        assertArrayEquals(new String[] {"pdf", "docx"}, config.getAllowedFiles());
    }
}
