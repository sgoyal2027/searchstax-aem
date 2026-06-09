package com.searchstax.aem.connector.core.servlets;

import com.adobe.granite.crypto.CryptoSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.ApiConfigService;
import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.FullIndexConfigService;
import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.impl.LanguageConfigServiceImpl;
import com.searchstax.aem.connector.core.config.model.FullIndexIncludePathConfig;
import com.searchstax.aem.connector.core.config.model.FullIndexConfig;
import com.searchstax.aem.connector.core.config.model.ApiConfig;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.incremental.IndexingAuditRecord;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexRunService;
import com.searchstax.aem.connector.core.services.FullIndexTriggerResult;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletResponse;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class WizardLoadServletsTest {

    private final AemContext context = AppAemContext.newAemContext();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private InitialSetupConfigService initialSetupConfigService;

    @Mock
    private ApiConfigService apiConfigService;

    @Mock
    private EmailConfigService emailConfigService;

    @Mock
    private IndexingAuditService indexingAuditService;

    @Mock
    private SearchStaxFullIndexRunService fullIndexRunService;

    @Mock
    private FullIndexConfigService fullIndexConfigService;

    @Mock
    private CryptoSupport cryptoSupport;

    @BeforeEach
    void setUp() {
        context.registerService(InitialSetupConfigService.class, initialSetupConfigService);
        context.registerService(ApiConfigService.class, apiConfigService);
        context.registerService(EmailConfigService.class, emailConfigService);
        context.registerService(IndexingAuditService.class, indexingAuditService);
        context.registerService(SearchStaxFullIndexRunService.class, fullIndexRunService);
        context.registerService(FullIndexConfigService.class, fullIndexConfigService);
        context.registerService(CryptoSupport.class, cryptoSupport);
    }

    @Test
    void initialSetupLoadServletReturnsConfiguredValues() throws Exception {
        final InitialSetupConfig config = new InitialSetupConfig();
        config.setEnableConnector(true);
        config.setRootPaths(new String[] {"/content/wknd"});
        config.setExcludePaths(new String[] {"/content/wknd/x"});
        config.setAllowedFiles(new String[] {"pdf"});
        when(initialSetupConfigService.getConfiguration()).thenReturn(config);

        final InitialSetupLoadServlet servlet = context.registerInjectActivateService(new InitialSetupLoadServlet());
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertTrue(json.get("enableConnector").asBoolean());
        assertEquals("/content/wknd", json.get("rootPaths").get(0).asText());
        assertEquals("pdf", json.get("allowedFiles").get(0).asText());
    }

    @Test
    void apiConfigLoadServletMasksSecrets() throws Exception {
        final ApiConfig config = new ApiConfig();
        config.setEndpointUrl("https://api.example.com");
        config.setUpdateEndpoint("https://update.example.com");
        config.setUpdateToken("secret");
        when(apiConfigService.getConfiguration()).thenReturn(config);

        final ApiConfigLoadServlet servlet = context.registerInjectActivateService(new ApiConfigLoadServlet());
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertEquals("https://api.example.com", json.get("endpointUrl").asText());
        assertEquals("https://update.example.com", json.get("updateEndpoint").asText());
        assertEquals("", json.get("updateToken").asText());
        assertTrue(json.get("updateTokenConfigured").asBoolean());
        assertFalse(json.get("apiTokenConfigured").asBoolean());
    }

    @Test
    void emailConfigLoadServletReturnsSmtpSettingsWithoutPassword() throws Exception {
        final EmailConfig config = new EmailConfig();
        config.setSmtpHost("smtp.gmail.com");
        config.setSmtpPort(587);
        config.setFromEmail("from@example.com");
        config.setReceiverEmails("ops@example.com");
        config.setNotifyOnIndexingFailure(true);
        config.setSmtpPassword("stored-secret");
        when(emailConfigService.getConfiguration()).thenReturn(config);

        final EmailConfigLoadServlet servlet = context.registerInjectActivateService(new EmailConfigLoadServlet());
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertEquals("smtp.gmail.com", json.get("smtpHost").asText());
        assertEquals(587, json.get("smtpPort").asInt());
        assertEquals("", json.get("smtpPassword").asText());
        assertTrue(json.get("smtpPasswordConfigured").asBoolean());
        assertTrue(json.get("notifyOnIndexingFailure").asBoolean());
    }

    @Test
    void emailConfigLoadServletReportsPasswordNotConfiguredWhenMissing() throws Exception {
        final EmailConfig config = new EmailConfig();
        config.setSmtpHost("smtp.gmail.com");
        config.setSmtpPort(587);
        when(emailConfigService.getConfiguration()).thenReturn(config);

        final EmailConfigLoadServlet servlet = context.registerInjectActivateService(new EmailConfigLoadServlet());
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertFalse(json.get("smtpPasswordConfigured").asBoolean());
    }

    @Test
    void indexingReportServletReturnsAuditRecords() throws Exception {
        final List<IndexingAuditRecord> records = Collections.singletonList(
                new IndexingAuditRecord(
                        "2026-06-07T10:00:00Z",
                        "/content/wknd/us/en/page",
                        IndexingAction.INDEX,
                        "SUCCESS",
                        "batch-1",
                        200,
                        "OK",
                        42L,
                        "doc-1"));

        when(indexingAuditService.getRecordsForLast24Hours()).thenReturn(records);

        final IndexingReportServlet servlet = context.registerInjectActivateService(new IndexingReportServlet());
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertEquals(1, json.size());
        assertEquals("SUCCESS", json.get(0).get("status").asText());
        assertEquals("INDEX", json.get(0).get("action").asText());
    }

    @Test
    void fullIndexRunServletReturnsServiceResponse() throws Exception {
        when(fullIndexRunService.triggerFullIndex(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new FullIndexTriggerResult(false, "", "Not available", 503));

        context.request().addRequestParameter("rootPath", "/content/wknd");

        final SearchStaxFullIndexRunServlet servlet =
                context.registerInjectActivateService(new SearchStaxFullIndexRunServlet());
        servlet.doPost(context.request(), context.response());

        final MockSlingHttpServletResponse response = context.response();
        assertEquals(503, response.getStatus());
        final JsonNode json = objectMapper.readTree(response.getOutputAsString());
        assertFalse(json.get("accepted").asBoolean());
        assertEquals("Not available", json.get("message").asText());
    }

    @Test
    void languageMappingLoadServletReturnsStoredJson() throws Exception {
        context.create().resource(
                LanguageConfigServiceImpl.CONFIG_PATH,
                "languageMappings",
                "[{\"aemLanguage\":\"en\",\"searchStaxLanguage\":\"abc\",\"enabled\":true}]");

        final LanguageMappingLoadServlet servlet =
                context.registerInjectActivateService(new LanguageMappingLoadServlet());
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertEquals("en", json.get(0).get("aemLanguage").asText());
    }

    @Test
    void metadataMappingLoadServletReturnsEmptyArrayWhenMissing() throws Exception {
        final MetadataMappingLoadServlet servlet =
                context.registerInjectActivateService(new MetadataMappingLoadServlet());
        servlet.doGet(context.request(), context.response());

        assertEquals("[]", context.response().getOutputAsString());
    }

    @Test
    void fullIndexConfigLoadServletReturnsConfiguredPaths() throws Exception {
        final FullIndexConfig config = new FullIndexConfig();
        config.setRootPath("/content/wknd");
        final FullIndexIncludePathConfig includePath = new FullIndexIncludePathConfig();
        includePath.setPath("/content/wknd/us/en");
        includePath.setIncludeChildPath(true);
        config.setIncludePaths(Collections.singletonList(includePath));
        config.setExcludePaths(new String[] {"/content/wknd/x"});
        when(fullIndexConfigService.getConfiguration()).thenReturn(config);

        final FullIndexConfigLoadServlet servlet =
                context.registerInjectActivateService(new FullIndexConfigLoadServlet());
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertEquals("/content/wknd", json.get("rootPath").asText());
        assertEquals("/content/wknd/us/en", json.get("includePaths").get(0).get("path").asText());
    }

    @Test
    void initialSetupLoadServletReturnsErrorWhenServiceFails() throws Exception {
        doThrow(new RuntimeException("config unavailable"))
                .when(initialSetupConfigService).getConfiguration();

        final InitialSetupLoadServlet servlet = context.registerInjectActivateService(new InitialSetupLoadServlet());
        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("Unable to load configuration"));
    }

    @Test
    void apiConfigLoadServletReturnsErrorWhenServiceFails() throws Exception {
        doThrow(new RuntimeException("config unavailable")).when(apiConfigService).getConfiguration();

        final ApiConfigLoadServlet servlet = context.registerInjectActivateService(new ApiConfigLoadServlet());
        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("Unable to load configuration"));
    }

    @Test
    void emailConfigLoadServletReturnsErrorWhenServiceFails() throws Exception {
        doThrow(new RuntimeException("config unavailable")).when(emailConfigService).getConfiguration();

        final EmailConfigLoadServlet servlet = context.registerInjectActivateService(new EmailConfigLoadServlet());
        servlet.doGet(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("Unable to load configuration"));
    }

    @Test
    void fullIndexConfigLoadServletReturnsDefaultRootPathWhenUnset() throws Exception {
        when(fullIndexConfigService.getConfiguration()).thenReturn(new FullIndexConfig());

        final FullIndexConfigLoadServlet servlet =
                context.registerInjectActivateService(new FullIndexConfigLoadServlet());
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertEquals("", json.get("rootPath").asText());
        assertTrue(json.get("includePaths").isArray());
    }

    @Test
    void metadataMappingLoadServletReturnsStoredJson() throws Exception {
        context.create().resource(
                "/conf/searchstaxconnector/settings/metadatafieldmapping",
                "metadataMappings",
                "[{\"aemField\":\"jcr:title\"}]");

        final MetadataMappingLoadServlet servlet =
                context.registerInjectActivateService(new MetadataMappingLoadServlet());
        servlet.doGet(context.request(), context.response());

        assertTrue(context.response().getOutputAsString().contains("jcr:title"));
    }

    @Test
    void fullIndexConfigLoadServletReturnsEmptyExcludePathsWhenUnset() throws Exception {
        final FullIndexConfig config = new FullIndexConfig();
        config.setRootPath("/content/wknd");
        config.setExcludePaths(null);
        when(fullIndexConfigService.getConfiguration()).thenReturn(config);

        final FullIndexConfigLoadServlet servlet =
                context.registerInjectActivateService(new FullIndexConfigLoadServlet());
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertTrue(json.get("excludePaths").isArray());
        assertEquals(0, json.get("excludePaths").size());
    }

    @Test
    void connectionTestServletRejectsMissingParameters() throws Exception {
        final SearchStaxConnectionTestServlet servlet =
                context.registerInjectActivateService(new SearchStaxConnectionTestServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("Missing required parameters"));
    }
}
