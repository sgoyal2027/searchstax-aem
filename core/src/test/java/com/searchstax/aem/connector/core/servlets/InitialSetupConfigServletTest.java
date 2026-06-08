package com.searchstax.aem.connector.core.servlets;

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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class InitialSetupConfigServletTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    @BeforeEach
    void setUp() throws Exception {
        context.create().resource("/conf/searchstaxconnector/settings");
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
    }

    @Test
    void rejectsRequestWithoutRootPaths() throws Exception {
        final InitialSetupConfigServlet servlet = new InitialSetupConfigServlet();
        TestReflection.inject(servlet, "resolverUtil", resolverUtil);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("At least one root path is required"));
    }

    @Test
    void rejectsBlankRootPathEntry() throws Exception {
        context.request().addRequestParameter("rootPaths", " ");

        final InitialSetupConfigServlet servlet = new InitialSetupConfigServlet();
        TestReflection.inject(servlet, "resolverUtil", resolverUtil);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("At least one root path is required"));
    }

    @Test
    void returnsInternalErrorWhenServiceLoginFails() throws Exception {
        context.request().addRequestParameter("rootPaths", "/content/wknd");
        when(resolverUtil.getServiceResolver()).thenThrow(new LoginException("login failed"));

        final InitialSetupConfigServlet servlet = new InitialSetupConfigServlet();
        TestReflection.inject(servlet, "resolverUtil", resolverUtil);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("service user mapping"));
    }

    @Test
    void rejectsExcludePathOutsideRootPaths() throws Exception {
        context.request().addRequestParameter("rootPaths", "/content/wknd");
        context.request().addRequestParameter("excludePaths", "/content/other");

        final InitialSetupConfigServlet servlet = new InitialSetupConfigServlet();
        TestReflection.inject(servlet, "resolverUtil", resolverUtil);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("Exclude path must be under"));
    }

    @Test
    void savesValidInitialSetupConfiguration() throws Exception {
        context.request().addRequestParameter("./enableConnector", "true");
        context.request().addRequestParameter("rootPaths", "/content/wknd");
        context.request().addRequestParameter("excludePaths", "/content/wknd/private");
        context.request().addRequestParameter("allowedFiles", "pdf");

        final InitialSetupConfigServlet servlet = new InitialSetupConfigServlet();
        TestReflection.inject(servlet, "resolverUtil", resolverUtil);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("\"success\":true"));
    }

    @Test
    void returnsInternalErrorWhenPersistenceExceptionThrown() throws Exception {
        context.request().addRequestParameter("rootPaths", "/content/wknd");
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        final Resource parent = org.mockito.Mockito.mock(Resource.class);

        when(resolver.getResource("/conf/searchstaxconnector/settings/initialsetupconfig")).thenReturn(null);
        when(resolver.getResource("/conf/searchstaxconnector/settings")).thenReturn(parent);
        org.mockito.Mockito.doThrow(new PersistenceException("persist failed"))
                .when(resolver)
                .create(org.mockito.ArgumentMatchers.any(Resource.class), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyMap());

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);

        final InitialSetupConfigServlet servlet = new InitialSetupConfigServlet();
        TestReflection.inject(servlet, "resolverUtil", resolverUtil);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("Unable to save configuration."));
    }

    @Test
    void returnsInternalErrorWhenConfigResourceCannotBeCreated() throws Exception {
        context.request().addRequestParameter("rootPaths", "/content/wknd");

        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        when(resolver.getResource("/conf/searchstaxconnector/settings/initialsetupconfig")).thenReturn(null);
        when(resolver.getResource("/conf/searchstaxconnector/settings")).thenReturn(null);
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);

        final InitialSetupConfigServlet servlet = new InitialSetupConfigServlet();
        TestReflection.inject(servlet, "resolverUtil", resolverUtil);
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void returnsInternalErrorWhenPropertiesCannotBeAdapted() throws Exception {
        context.request().addRequestParameter("rootPaths", "/content/wknd");

        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        final Resource resource = org.mockito.Mockito.mock(Resource.class);

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource("/conf/searchstaxconnector/settings/initialsetupconfig")).thenReturn(null);
        when(resolver.getResource("/conf/searchstaxconnector/settings"))
                .thenReturn(context.resourceResolver().getResource("/conf/searchstaxconnector/settings"));
        when(resolver.create(
                org.mockito.ArgumentMatchers.any(Resource.class),
                org.mockito.ArgumentMatchers.eq("initialsetupconfig"),
                org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(resource);
        when(resource.adaptTo(org.apache.sling.api.resource.ModifiableValueMap.class)).thenReturn(null);

        final InitialSetupConfigServlet servlet = new InitialSetupConfigServlet();
        TestReflection.inject(servlet, "resolverUtil", resolverUtil);
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void savesConfigurationWithoutExcludeAndAllowedFiles() throws Exception {
        context.request().addRequestParameter("./enableConnector", "on");
        context.request().addRequestParameter("rootPaths", "/content/wknd");
        context.create().resource(
                "/conf/searchstaxconnector/settings/initialsetupconfig",
                "excludePaths",
                new String[] {"/content/wknd/x"},
                "allowedFiles",
                new String[] {"pdf"});

        final InitialSetupConfigServlet servlet = new InitialSetupConfigServlet();
        TestReflection.inject(servlet, "resolverUtil", resolverUtil);
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        final org.apache.sling.api.resource.Resource resource = context.resourceResolver()
                .getResource("/conf/searchstaxconnector/settings/initialsetupconfig");
        assertFalse(resource.getValueMap().containsKey("excludePaths"));
        assertFalse(resource.getValueMap().containsKey("allowedFiles"));
    }

    @Test
    void skipsBlankExcludePathValidation() throws Exception {
        context.request().addRequestParameter("rootPaths", "/content/wknd");
        context.request().addRequestParameter("excludePaths", " ");

        final InitialSetupConfigServlet servlet = new InitialSetupConfigServlet();
        TestReflection.inject(servlet, "resolverUtil", resolverUtil);
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void returnsInternalErrorWhenUnexpectedExceptionThrown() throws Exception {
        context.request().addRequestParameter("rootPaths", "/content/wknd");
        when(resolverUtil.getServiceResolver()).thenThrow(new RuntimeException("boom"));

        final InitialSetupConfigServlet servlet = new InitialSetupConfigServlet();
        TestReflection.inject(servlet, "resolverUtil", resolverUtil);

        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("Unexpected error occurred."));
    }
}
