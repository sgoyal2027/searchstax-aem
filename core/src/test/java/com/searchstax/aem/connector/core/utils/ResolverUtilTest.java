package com.searchstax.aem.connector.core.utils;

import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.searchstax.aem.connector.core.testcontext.TestReflection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class ResolverUtilTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Test
    void getServiceResolverUsesSubserviceName() throws Exception {
        final ResourceResolverFactory factory = mock(ResourceResolverFactory.class);
        when(factory.getServiceResourceResolver(anyMap())).thenReturn(context.resourceResolver());

        final ResolverUtil resolverUtil = new ResolverUtil();
        TestReflection.inject(resolverUtil, "resourceResolverFactory", factory);

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            assertNotNull(resolver);
        }
    }

    @Test
    void getServiceResolverPropagatesLoginException() throws Exception {
        final ResourceResolverFactory factory = mock(ResourceResolverFactory.class);
        when(factory.getServiceResourceResolver(anyMap())).thenThrow(new LoginException("denied"));

        final ResolverUtil resolverUtil = new ResolverUtil();
        com.searchstax.aem.connector.core.testcontext.TestReflection.inject(
                resolverUtil, "resourceResolverFactory", factory);

        org.junit.jupiter.api.Assertions.assertThrows(
                LoginException.class,
                resolverUtil::getServiceResolver);
    }
}
