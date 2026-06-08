package com.searchstax.aem.connector.core.servlets;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.searchstax.aem.connector.core.config.impl.ApiConfigServiceImpl;
import com.searchstax.aem.connector.core.config.impl.EmailConfigServiceImpl;
import com.searchstax.aem.connector.core.config.impl.LanguageConfigServiceImpl;
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
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class WizardSaveServletsTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private CryptoSupport cryptoSupport;

    @BeforeEach
    void setUp() throws Exception {
        context.create().resource("/conf/searchstaxconnector/settings");
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
        when(cryptoSupport.protect(anyString())).thenAnswer(invocation -> "protected-" + invocation.getArgument(0));
    }

    @Test
    void apiConfigSaveServletRejectsMissingRequiredFields() throws Exception {
        final ApiConfigSaveServlet servlet = activate(new ApiConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void apiConfigSaveServletSavesConfigurationAndPreservesBlankTokens() throws Exception {
        context.create().resource(
                ApiConfigServiceImpl.CONFIG_PATH,
                "apiToken", "existing-token",
                "updateToken", "existing-update");

        context.request().setParameterMap(Map.of(
                "endpointUrl", "https://api.example.com",
                "selectEndpoint", "https://select.example.com/emselect",
                "updateEndpoint", "https://update.example.com",
                "apiToken", "",
                "updateToken", "new-update-token"));

        final ApiConfigSaveServlet servlet = activate(new ApiConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("\"success\":true"));
        assertEquals("existing-token", context.resourceResolver()
                .getResource(ApiConfigServiceImpl.CONFIG_PATH)
                .getValueMap()
                .get("apiToken", String.class));
        assertEquals("protected-new-update-token", context.resourceResolver()
                .getResource(ApiConfigServiceImpl.CONFIG_PATH)
                .getValueMap()
                .get("updateToken", String.class));
    }

    @Test
    void apiConfigSaveServletReturns500WhenConfigPathMissing() throws Exception {
        context.request().setParameterMap(Map.of(
                "endpointUrl", "https://api.example.com",
                "selectEndpoint", "https://select.example.com",
                "updateEndpoint", "https://update.example.com"));

        final ApiConfigSaveServlet servlet = activate(new ApiConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void apiConfigSaveServletStoresPlaintextWhenEncryptionFails() throws Exception {
        context.create().resource(ApiConfigServiceImpl.CONFIG_PATH);
        when(cryptoSupport.protect("secret")).thenThrow(new CryptoException("fail"));

        context.request().setParameterMap(Map.of(
                "endpointUrl", "https://api.example.com",
                "selectEndpoint", "https://select.example.com",
                "updateEndpoint", "https://update.example.com",
                "apiToken", "secret"));

        final ApiConfigSaveServlet servlet = activate(new ApiConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals("secret", context.resourceResolver()
                .getResource(ApiConfigServiceImpl.CONFIG_PATH)
                .getValueMap()
                .get("apiToken", String.class));
    }

    @Test
    void emailConfigSaveServletRejectsMissingSmtpHost() throws Exception {
        context.request().addRequestParameter("smtpPort", "587");
        context.request().addRequestParameter("receiverEmails", "ops@example.com");

        final EmailConfigSaveServlet servlet = activate(new EmailConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void emailConfigSaveServletSavesConfiguration() throws Exception {
        context.request().setParameterMap(Map.of(
                "smtpHost", "smtp.gmail.com",
                "smtpPort", "587",
                "smtpUser", "user@example.com",
                "smtpPassword", "app-password",
                "fromEmail", "from@example.com",
                "receiverEmails", "ops@example.com, dev@example.com",
                "smtpUseSSL", "true",
                "notifyOnIndexingFailure", "on"));

        final EmailConfigSaveServlet servlet = activate(new EmailConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals("smtp.gmail.com", context.resourceResolver()
                .getResource(EmailConfigServiceImpl.CONFIG_PATH)
                .getValueMap()
                .get("smtpHost", String.class));
    }

    @Test
    void emailConfigSaveServletPreservesExistingPasswordWhenBlank() throws Exception {
        context.create().resource(
                EmailConfigServiceImpl.CONFIG_PATH,
                "smtpPassword", "stored-password");

        context.request().setParameterMap(Map.of(
                "smtpHost", "smtp.gmail.com",
                "smtpPort", "587",
                "receiverEmails", "ops@example.com",
                "smtpPassword", ""));

        final EmailConfigSaveServlet servlet = activate(new EmailConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals("stored-password", context.resourceResolver()
                .getResource(EmailConfigServiceImpl.CONFIG_PATH)
                .getValueMap()
                .get("smtpPassword", String.class));
    }

    @Test
    void fullIndexConfigSaveServletRejectsMissingRootPath() throws Exception {
        final FullIndexConfigSaveServlet servlet = activate(new FullIndexConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void fullIndexConfigSaveServletRejectsIncludePathOutsideRoot() throws Exception {
        context.request().addRequestParameter("./rootPath", "/content/wknd");
        context.request().addRequestParameter(
                "./includePathsJson",
                "[{\"path\":\"/content/other\",\"includeChildPath\":true}]");

        final FullIndexConfigSaveServlet servlet = activate(new FullIndexConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void fullIndexConfigSaveServletSavesIncludeAndExcludePaths() throws Exception {
        context.create().resource("/conf/searchstaxconnector/settings/fullindexsetupconfig");

        context.request().addRequestParameter("./rootPath", "/content/wknd");
        context.request().addRequestParameter(
                "./includePathsJson",
                "[{\"path\":\"/content/wknd/us/en\",\"includeChildPath\":true}]");
        context.request().addRequestParameter("./excludePaths", "/content/wknd/private");

        final FullIndexConfigSaveServlet servlet = activate(new FullIndexConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals("/content/wknd", context.resourceResolver()
                .getResource("/conf/searchstaxconnector/settings/fullindexsetupconfig")
                .getValueMap()
                .get("rootPath", String.class));
    }

    @Test
    void languageMappingSaveServletPersistsMappings() throws Exception {
        context.request().setParameterMap(Map.of(
                "languageMappings/item0/aemLanguageType", "en",
                "languageMappings/item0/searchStaxLanguage", "abc",
                "languageMappings/item0/enabled", "true"));

        final LanguageMappingSaveServlet servlet = activate(new LanguageMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        final String json = context.resourceResolver()
                .getResource(LanguageConfigServiceImpl.CONFIG_PATH)
                .getValueMap()
                .get("languageMappings", String.class);
        assertTrue(json.contains("\"aemLanguage\":\"en\""));
    }

    @Test
    void languageMappingSaveServletUsesCustomAemLanguage() throws Exception {
        context.request().setParameterMap(Map.of(
                "languageMappings/item0/aemLanguageType", "custom",
                "languageMappings/item0/customAemLanguage", "en_GB",
                "languageMappings/item0/searchStaxLanguage", "gb"));

        final LanguageMappingSaveServlet servlet = activate(new LanguageMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        final String json = context.resourceResolver()
                .getResource(LanguageConfigServiceImpl.CONFIG_PATH)
                .getValueMap()
                .get("languageMappings", String.class);
        assertTrue(json.contains("\"aemLanguage\":\"en_GB\""));
    }

    @Test
    void metadataMappingSaveServletPersistsMappings() throws Exception {
        context.request().setParameterMap(Map.of(
                "metadataMappings/item0/mappingType", "jcr:title",
                "metadataMappings/item0/indexFieldName", "title",
                "metadataMappings/item0/fieldType", "text",
                "metadataMappings/item0/enabled", "true"));

        final MetadataMappingSaveServlet servlet = activate(new MetadataMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        final String json = context.resourceResolver()
                .getResource("/conf/searchstaxconnector/settings/metadatafieldmapping")
                .getValueMap()
                .get("metadataMappings", String.class);
        assertTrue(json.contains("\"aemField\":\"jcr:title\""));
        assertTrue(json.contains("\"enabled\":true"));
    }

    @Test
    void metadataMappingSaveServletPersistsCustomPropertyForCustomField() throws Exception {
        context.request().setParameterMap(Map.of(
                "metadataMappings/item0/mappingType", "custom",
                "metadataMappings/item0/customProperty", "myproject:seoTitle",
                "metadataMappings/item0/indexFieldName", "seo_title",
                "metadataMappings/item0/fieldType", "text",
                "metadataMappings/item0/enabled", "true"));

        final MetadataMappingSaveServlet servlet = activate(new MetadataMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        final String json = context.resourceResolver()
                .getResource("/conf/searchstaxconnector/settings/metadatafieldmapping")
                .getValueMap()
                .get("metadataMappings", String.class);
        assertTrue(json.contains("\"aemField\":\"custom\""));
        assertTrue(json.contains("\"customProperty\":\"myproject:seoTitle\""));
    }

    @Test
    void metadataMappingSaveServletClearsCustomPropertyForPresetField() throws Exception {
        context.request().setParameterMap(Map.of(
                "metadataMappings/item0/mappingType", "jcr:description",
                "metadataMappings/item0/customProperty", "jcr:description",
                "metadataMappings/item0/indexFieldName", "description",
                "metadataMappings/item0/fieldType", "text",
                "metadataMappings/item0/enabled", "true"));

        final MetadataMappingSaveServlet servlet = activate(new MetadataMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        final String json = context.resourceResolver()
                .getResource("/conf/searchstaxconnector/settings/metadatafieldmapping")
                .getValueMap()
                .get("metadataMappings", String.class);
        assertTrue(json.contains("\"aemField\":\"jcr:description\""));
        assertTrue(json.contains("\"customProperty\":\"\""));
    }

    @Test
    void metadataMappingSaveServletPersistsExplicitlyDisabledEnabled() throws Exception {
        context.request().setParameterMap(Map.of(
                "metadataMappings/item0/mappingType", "jcr:title",
                "metadataMappings/item0/indexFieldName", "title",
                "metadataMappings/item0/fieldType", "text",
                "metadataMappings/item0/enabled", "false"));

        final MetadataMappingSaveServlet servlet = activate(new MetadataMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        final String json = context.resourceResolver()
                .getResource("/conf/searchstaxconnector/settings/metadatafieldmapping")
                .getValueMap()
                .get("metadataMappings", String.class);
        assertTrue(json.contains("\"enabled\":false"));
    }

    @Test
    void metadataMappingSaveServletDefaultsEnabledToFalseWhenMissing() throws Exception {
        context.request().setParameterMap(Map.of(
                "metadataMappings/item0/mappingType", "jcr:title",
                "metadataMappings/item0/indexFieldName", "title",
                "metadataMappings/item0/fieldType", "text"));

        final MetadataMappingSaveServlet servlet = activate(new MetadataMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        final String json = context.resourceResolver()
                .getResource("/conf/searchstaxconnector/settings/metadatafieldmapping")
                .getValueMap()
                .get("metadataMappings", String.class);
        assertTrue(json.contains("\"enabled\":false"));
    }

    @Test
    void languageMappingSaveServletDefaultsEnabledToFalseWhenMissing() throws Exception {
        context.request().setParameterMap(Map.of(
                "languageMappings/item0/aemLanguageType", "en",
                "languageMappings/item0/searchStaxLanguage", "en"));

        final LanguageMappingSaveServlet servlet = activate(new LanguageMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        final String json = context.resourceResolver()
                .getResource(LanguageConfigServiceImpl.CONFIG_PATH)
                .getValueMap()
                .get("languageMappings", String.class);
        assertTrue(json.contains("\"enabled\":false"));
    }

    @Test
    void apiConfigSaveServletReturns500WhenPropertiesCannotBeAdapted() throws Exception {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        final Resource resource = org.mockito.Mockito.mock(Resource.class);

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(ApiConfigServiceImpl.CONFIG_PATH)).thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(null);

        context.request().setParameterMap(Map.of(
                "endpointUrl", "https://api.example.com",
                "selectEndpoint", "https://select.example.com",
                "updateEndpoint", "https://update.example.com"));

        final ApiConfigSaveServlet servlet = activate(new ApiConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void apiConfigSaveServletReturns500WhenCommitThrowsPersistenceException() throws Exception {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        final Resource resource = org.mockito.Mockito.mock(Resource.class);
        final ModifiableValueMap properties = org.mockito.Mockito.mock(ModifiableValueMap.class);

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(ApiConfigServiceImpl.CONFIG_PATH)).thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(Map.of()));
        org.mockito.Mockito.doThrow(new PersistenceException("commit failed"))
                .when(resolver)
                .commit();

        context.request().setParameterMap(Map.of(
                "endpointUrl", "https://api.example.com",
                "selectEndpoint", "https://select.example.com",
                "updateEndpoint", "https://update.example.com"));

        final ApiConfigSaveServlet servlet = activate(new ApiConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void apiConfigSaveServletReturns500WhenUnexpectedExceptionThrown() throws Exception {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(ApiConfigServiceImpl.CONFIG_PATH))
                .thenThrow(new RuntimeException("boom"));

        context.request().setParameterMap(Map.of(
                "endpointUrl", "https://api.example.com",
                "selectEndpoint", "https://select.example.com",
                "updateEndpoint", "https://update.example.com"));

        final ApiConfigSaveServlet servlet = activate(new ApiConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void emailConfigSaveServletRejectsMissingReceiverEmails() throws Exception {
        context.request().setParameterMap(Map.of(
                "smtpHost", "smtp.gmail.com",
                "smtpPort", "587"));

        final EmailConfigSaveServlet servlet = activate(new EmailConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void emailConfigSaveServletReturns500WhenConfigPathMissing() throws Exception {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(EmailConfigServiceImpl.CONFIG_PATH)).thenReturn(null);
        when(resolver.getResource("/conf/searchstaxconnector/settings")).thenReturn(null);

        context.request().setParameterMap(Map.of(
                "smtpHost", "smtp.gmail.com",
                "smtpPort", "587",
                "receiverEmails", "ops@example.com"));

        final EmailConfigSaveServlet servlet = activate(new EmailConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void emailConfigSaveServletReturns500WhenPropertiesCannotBeAdapted() throws Exception {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        final Resource resource = org.mockito.Mockito.mock(Resource.class);

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(EmailConfigServiceImpl.CONFIG_PATH)).thenReturn(null);
        when(resolver.getResource("/conf/searchstaxconnector/settings"))
                .thenReturn(context.resourceResolver().getResource("/conf/searchstaxconnector/settings"));
        when(resolver.create(
                org.mockito.ArgumentMatchers.any(Resource.class),
                org.mockito.ArgumentMatchers.eq("emailconfig"),
                org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(null);

        context.request().setParameterMap(Map.of(
                "smtpHost", "smtp.gmail.com",
                "smtpPort", "587",
                "receiverEmails", "ops@example.com"));

        final EmailConfigSaveServlet servlet = activate(new EmailConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void emailConfigSaveServletStoresPlaintextWhenEncryptionFails() throws Exception {
        context.create().resource(EmailConfigServiceImpl.CONFIG_PATH);
        when(cryptoSupport.protect("secret")).thenThrow(new CryptoException("fail"));

        context.request().setParameterMap(Map.of(
                "smtpHost", "smtp.gmail.com",
                "smtpPort", "587",
                "receiverEmails", "ops@example.com",
                "smtpPassword", "secret"));

        final EmailConfigSaveServlet servlet = activate(new EmailConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals("secret", context.resourceResolver()
                .getResource(EmailConfigServiceImpl.CONFIG_PATH)
                .getValueMap()
                .get("smtpPassword", String.class));
    }

    @Test
    void emailConfigSaveServletRemovesPasswordWhenBlankAndNoExistingValue() throws Exception {
        context.create().resource(EmailConfigServiceImpl.CONFIG_PATH);

        context.request().setParameterMap(Map.of(
                "smtpHost", "smtp.gmail.com",
                "smtpPort", "invalid",
                "receiverEmails", "ops@example.com",
                "smtpPassword", ""));

        final EmailConfigSaveServlet servlet = activate(new EmailConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(25, context.resourceResolver()
                .getResource(EmailConfigServiceImpl.CONFIG_PATH)
                .getValueMap()
                .get("smtpPort", Integer.class));
        assertFalse(context.resourceResolver()
                .getResource(EmailConfigServiceImpl.CONFIG_PATH)
                .getValueMap()
                .containsKey("smtpPassword"));
    }

    @Test
    void emailConfigSaveServletReturns500WhenPersistenceFails() throws Exception {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        final Resource resource = org.mockito.Mockito.mock(Resource.class);
        final ModifiableValueMap properties = org.mockito.Mockito.mock(ModifiableValueMap.class);

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(EmailConfigServiceImpl.CONFIG_PATH)).thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(Map.of()));
        org.mockito.Mockito.doThrow(new PersistenceException("commit failed")).when(resolver).commit();

        context.request().setParameterMap(Map.of(
                "smtpHost", "smtp.gmail.com",
                "smtpPort", "587",
                "receiverEmails", "ops@example.com"));

        final EmailConfigSaveServlet servlet = activate(new EmailConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void emailConfigSaveServletReturns500WhenUnexpectedExceptionThrown() throws Exception {
        when(resolverUtil.getServiceResolver()).thenThrow(new RuntimeException("boom"));

        context.request().setParameterMap(Map.of(
                "smtpHost", "smtp.gmail.com",
                "smtpPort", "587",
                "receiverEmails", "ops@example.com"));

        final EmailConfigSaveServlet servlet = activate(new EmailConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void emailConfigSaveServletProtectsWithoutCryptoSupport() throws Exception {
        context.create().resource(EmailConfigServiceImpl.CONFIG_PATH);

        context.request().setParameterMap(Map.of(
                "smtpHost", "smtp.gmail.com",
                "smtpPort", "587",
                "receiverEmails", "ops@example.com",
                "smtpPassword", "plain"));

        final EmailConfigSaveServlet servlet = new EmailConfigSaveServlet();
        TestReflection.inject(servlet, "resolverUtil", resolverUtil);
        servlet.doPost(context.request(), context.response());

        assertEquals("plain", context.resourceResolver()
                .getResource(EmailConfigServiceImpl.CONFIG_PATH)
                .getValueMap()
                .get("smtpPassword", String.class));
    }

    @Test
    void fullIndexConfigSaveServletRejectsExcludePathOutsideRoot() throws Exception {
        context.request().addRequestParameter("./rootPath", "/content/wknd");
        context.request().addRequestParameter("./excludePaths", "/content/other");

        final FullIndexConfigSaveServlet servlet = activate(new FullIndexConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void fullIndexConfigSaveServletReturns500WhenConfigPathMissing() throws Exception {
        context.request().addRequestParameter("./rootPath", "/content/wknd");

        final FullIndexConfigSaveServlet servlet = activate(new FullIndexConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void fullIndexConfigSaveServletReturns500WhenPropertiesCannotBeAdapted() throws Exception {
        context.create().resource("/conf/searchstaxconnector/settings/fullindexsetupconfig");
        final Resource resource = context.resourceResolver()
                .getResource("/conf/searchstaxconnector/settings/fullindexsetupconfig");
        final Resource spyResource = org.mockito.Mockito.spy(resource);
        org.mockito.Mockito.doReturn(null).when(spyResource).adaptTo(ModifiableValueMap.class);

        final ResourceResolver resolver = org.mockito.Mockito.spy(context.resourceResolver());
        org.mockito.Mockito.doReturn(spyResource)
                .when(resolver)
                .getResource("/conf/searchstaxconnector/settings/fullindexsetupconfig");
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);

        context.request().addRequestParameter("./rootPath", "/content/wknd");

        final FullIndexConfigSaveServlet servlet = activate(new FullIndexConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void fullIndexConfigSaveServletRemovesExcludePathsWhenBlank() throws Exception {
        context.create().resource(
                "/conf/searchstaxconnector/settings/fullindexsetupconfig",
                "excludePaths",
                new String[] {"/content/wknd/private"});

        context.request().addRequestParameter("./rootPath", "/content/wknd");

        final FullIndexConfigSaveServlet servlet = activate(new FullIndexConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertFalse(context.resourceResolver()
                .getResource("/conf/searchstaxconnector/settings/fullindexsetupconfig")
                .getValueMap()
                .containsKey("excludePaths"));
    }

    @Test
    void fullIndexConfigSaveServletReplacesExistingIncludePathsResource() throws Exception {
        context.create().resource("/conf/searchstaxconnector/settings/fullindexsetupconfig");
        context.create().resource("/conf/searchstaxconnector/settings/fullindexsetupconfig/includePaths");
        context.create().resource(
                "/conf/searchstaxconnector/settings/fullindexsetupconfig/includePaths/item0",
                "path",
                "/content/wknd/old");

        context.request().addRequestParameter("./rootPath", "/content/wknd");
        context.request().addRequestParameter(
                "./includePathsJson",
                "[{\"path\":\"/content/wknd/us/en\",\"includeChildPath\":true}]");

        final FullIndexConfigSaveServlet servlet = activate(new FullIndexConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals(
                "/content/wknd/us/en",
                context.resourceResolver()
                        .getResource(
                                "/conf/searchstaxconnector/settings/fullindexsetupconfig/includePaths/item0")
                        .getValueMap()
                        .get("path", String.class));
    }

    @Test
    void fullIndexConfigSaveServletReturns500WhenPersistenceFails() throws Exception {
        context.create().resource("/conf/searchstaxconnector/settings/fullindexsetupconfig");
        final ResourceResolver resolver = org.mockito.Mockito.spy(context.resourceResolver());
        org.mockito.Mockito.doThrow(new PersistenceException("commit failed")).when(resolver).commit();
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);

        context.request().addRequestParameter("./rootPath", "/content/wknd");

        final FullIndexConfigSaveServlet servlet = activate(new FullIndexConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void fullIndexConfigSaveServletReturns500WhenUnexpectedExceptionThrown() throws Exception {
        when(resolverUtil.getServiceResolver()).thenThrow(new RuntimeException("boom"));

        context.request().addRequestParameter("./rootPath", "/content/wknd");

        final FullIndexConfigSaveServlet servlet = activate(new FullIndexConfigSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void languageMappingSaveServletSkipsBlankAemLanguageType() throws Exception {
        context.request().setParameterMap(Map.of(
                "languageMappings/item0/aemLanguageType", " ",
                "languageMappings/item0/searchStaxLanguage", "abc"));

        final LanguageMappingSaveServlet servlet = activate(new LanguageMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals("[]", context.resourceResolver()
                .getResource(LanguageConfigServiceImpl.CONFIG_PATH)
                .getValueMap()
                .get("languageMappings", String.class));
    }

    @Test
    void languageMappingSaveServletReturns500WhenConfigPathMissing() throws Exception {
        context.request().setParameterMap(Map.of(
                "languageMappings/item0/aemLanguageType", "en",
                "languageMappings/item0/searchStaxLanguage", "abc"));

        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(LanguageConfigServiceImpl.CONFIG_PATH)).thenReturn(null);
        when(resolver.getResource("/conf/searchstaxconnector/settings")).thenReturn(null);

        final LanguageMappingSaveServlet servlet = activate(new LanguageMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void languageMappingSaveServletReturns500WhenPropertiesCannotBeAdapted() throws Exception {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        final Resource resource = org.mockito.Mockito.mock(Resource.class);

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(LanguageConfigServiceImpl.CONFIG_PATH)).thenReturn(null);
        when(resolver.getResource("/conf/searchstaxconnector/settings"))
                .thenReturn(context.resourceResolver().getResource("/conf/searchstaxconnector/settings"));
        when(resolver.create(
                org.mockito.ArgumentMatchers.any(Resource.class),
                org.mockito.ArgumentMatchers.eq("languagemapping"),
                org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(null);

        context.request().setParameterMap(Map.of(
                "languageMappings/item0/aemLanguageType", "en",
                "languageMappings/item0/searchStaxLanguage", "abc"));

        final LanguageMappingSaveServlet servlet = activate(new LanguageMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void languageMappingSaveServletReturns500WhenPersistenceFails() throws Exception {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        final Resource resource = org.mockito.Mockito.mock(Resource.class);
        final ModifiableValueMap properties = org.mockito.Mockito.mock(ModifiableValueMap.class);

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource(LanguageConfigServiceImpl.CONFIG_PATH)).thenReturn(null);
        when(resolver.getResource("/conf/searchstaxconnector/settings"))
                .thenReturn(context.resourceResolver().getResource("/conf/searchstaxconnector/settings"));
        when(resolver.create(
                org.mockito.ArgumentMatchers.any(Resource.class),
                org.mockito.ArgumentMatchers.eq("languagemapping"),
                org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);
        org.mockito.Mockito.doThrow(new PersistenceException("commit failed")).when(resolver).commit();

        context.request().setParameterMap(Map.of(
                "languageMappings/item0/aemLanguageType", "en",
                "languageMappings/item0/searchStaxLanguage", "abc"));

        final LanguageMappingSaveServlet servlet = activate(new LanguageMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void metadataMappingSaveServletSkipsBlankMappingType() throws Exception {
        context.request().setParameterMap(Map.of(
                "metadataMappings/item0/mappingType", " ",
                "metadataMappings/item0/indexFieldName", "title"));

        final MetadataMappingSaveServlet servlet = activate(new MetadataMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertEquals("[]", context.resourceResolver()
                .getResource("/conf/searchstaxconnector/settings/metadatafieldmapping")
                .getValueMap()
                .get("metadataMappings", String.class));
    }

    @Test
    void metadataMappingSaveServletReturns500WhenConfigPathMissing() throws Exception {
        context.request().setParameterMap(Map.of(
                "metadataMappings/item0/mappingType", "jcr:title",
                "metadataMappings/item0/indexFieldName", "title"));

        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource("/conf/searchstaxconnector/settings/metadatafieldmapping")).thenReturn(null);
        when(resolver.getResource("/conf/searchstaxconnector/settings")).thenReturn(null);

        final MetadataMappingSaveServlet servlet = activate(new MetadataMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void languageMappingSaveServletReturns500WhenUnexpectedExceptionThrown() throws Exception {
        when(resolverUtil.getServiceResolver()).thenThrow(new RuntimeException("boom"));

        context.request().setParameterMap(Map.of(
                "languageMappings/item0/aemLanguageType", "en",
                "languageMappings/item0/searchStaxLanguage", "abc"));

        final LanguageMappingSaveServlet servlet = activate(new LanguageMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void metadataMappingSaveServletReturns500WhenPropertiesCannotBeAdapted() throws Exception {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        final Resource resource = org.mockito.Mockito.mock(Resource.class);

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource("/conf/searchstaxconnector/settings/metadatafieldmapping")).thenReturn(null);
        when(resolver.getResource("/conf/searchstaxconnector/settings"))
                .thenReturn(context.resourceResolver().getResource("/conf/searchstaxconnector/settings"));
        when(resolver.create(
                org.mockito.ArgumentMatchers.any(Resource.class),
                org.mockito.ArgumentMatchers.eq("metadatafieldmapping"),
                org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(null);

        context.request().setParameterMap(Map.of(
                "metadataMappings/item0/mappingType", "jcr:title",
                "metadataMappings/item0/indexFieldName", "title"));

        final MetadataMappingSaveServlet servlet = activate(new MetadataMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void metadataMappingSaveServletReturns500WhenPersistenceFails() throws Exception {
        final ResourceResolver resolver = org.mockito.Mockito.mock(ResourceResolver.class);
        final Resource resource = org.mockito.Mockito.mock(Resource.class);
        final ModifiableValueMap properties = org.mockito.Mockito.mock(ModifiableValueMap.class);

        when(resolverUtil.getServiceResolver()).thenReturn(resolver);
        when(resolver.getResource("/conf/searchstaxconnector/settings/metadatafieldmapping")).thenReturn(null);
        when(resolver.getResource("/conf/searchstaxconnector/settings"))
                .thenReturn(context.resourceResolver().getResource("/conf/searchstaxconnector/settings"));
        when(resolver.create(
                org.mockito.ArgumentMatchers.any(Resource.class),
                org.mockito.ArgumentMatchers.eq("metadatafieldmapping"),
                org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(resource);
        when(resource.adaptTo(ModifiableValueMap.class)).thenReturn(properties);
        org.mockito.Mockito.doThrow(new PersistenceException("commit failed")).when(resolver).commit();

        context.request().setParameterMap(Map.of(
                "metadataMappings/item0/mappingType", "jcr:title",
                "metadataMappings/item0/indexFieldName", "title"));

        final MetadataMappingSaveServlet servlet = activate(new MetadataMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    @Test
    void metadataMappingSaveServletReturns500WhenUnexpectedExceptionThrown() throws Exception {
        when(resolverUtil.getServiceResolver()).thenThrow(new RuntimeException("boom"));

        context.request().setParameterMap(Map.of(
                "metadataMappings/item0/mappingType", "jcr:title",
                "metadataMappings/item0/indexFieldName", "title"));

        final MetadataMappingSaveServlet servlet = activate(new MetadataMappingSaveServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }

    private <T> T activate(final T servlet) {
        TestReflection.inject(servlet, "resolverUtil", resolverUtil);
        if (servlet instanceof ApiConfigSaveServlet || servlet instanceof EmailConfigSaveServlet) {
            TestReflection.inject(servlet, "cryptoSupport", cryptoSupport);
        }
        return servlet;
    }
}
