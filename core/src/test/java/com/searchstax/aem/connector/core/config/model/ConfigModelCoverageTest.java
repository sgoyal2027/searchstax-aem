package com.searchstax.aem.connector.core.config.model;

import com.searchstax.aem.connector.core.services.FullIndexProgress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigModelCoverageTest {

    @Test
    void apiConfigGettersAndSetters() {
        final ApiConfig config = new ApiConfig();
        config.setEndpointUrl("endpoint");
        config.setApiToken("token");
        config.setSelectEndpoint("select");
        config.setSelectToken("selectToken");
        config.setUpdateEndpoint("update");
        config.setUpdateToken("updateToken");
        config.setAutoSuggestApi("suggest");
        config.setRelatedSearchesEndpoint("related");
        config.setPopularSearchesEndpoint("popular");
        config.setDiscoveryApiKey("discovery");
        config.setAnalyticsTrackingUrl("track");
        config.setAnalyticsTrackingKey("trackKey");
        config.setAnalyticsReportingUrl("report");
        config.setAnalyticsReportingApiKey("reportKey");
        config.setForwardGeocodingEndpoint("forward");
        config.setReverseGeocodingEndpoint("reverse");

        assertEquals("endpoint", config.getEndpointUrl());
        assertEquals("token", config.getApiToken());
        assertEquals("reverse", config.getReverseGeocodingEndpoint());
    }

    @Test
    void emailConfigGettersAndSetters() {
        final EmailConfig config = new EmailConfig();
        config.setSmtpHost("host");
        config.setSmtpPort(465);
        config.setSmtpUser("user");
        config.setSmtpPassword("pass");
        config.setFromEmail("from@example.com");
        config.setReceiverEmails("ops@example.com");
        config.setSmtpUseSsl(true);
        config.setSmtpUseStartTls(true);
        config.setSmtpRequireStartTls(true);
        config.setDebugEmail(true);
        config.setOauthFlow(true);
        config.setNotifyOnIndexingFailure(false);

        assertEquals("host", config.getSmtpHost());
        assertTrue(config.isSmtpUseSsl());
        assertTrue(config.isDebugEmail());
        assertTrue(config.isOauthFlow());
        assertFalse(config.isNotifyOnIndexingFailure());
    }

    @Test
    void initialSetupConfigCopiesArrays() {
        final InitialSetupConfig config = new InitialSetupConfig();
        config.setEnableConnector(true);
        config.setRootPaths(new String[] {"/content/wknd"});
        config.setExcludePaths(new String[] {"/content/x"});
        config.setAllowedFiles(new String[] {"pdf"});

        assertTrue(config.isEnableConnector());
        assertArrayEquals(new String[] {"/content/wknd"}, config.getRootPaths());
    }

    @Test
    void metadataFieldMappingConfigGettersAndSetters() {
        final MetadataFieldMappingConfig config = new MetadataFieldMappingConfig();
        config.setAemField("jcr:title");
        config.setCustomProperty("custom");
        config.setSearchStaxField("title");
        config.setType("text");
        config.setEnabled(true);

        assertEquals("custom", config.getCustomProperty());
        assertTrue(config.isEnabled());
    }

    @Test
    void fullIndexConfigReturnsEmptyExcludePathsWhenUnset() {
        final FullIndexConfig config = new FullIndexConfig();
        config.setExcludePaths(null);

        assertArrayEquals(new String[0], config.getExcludePaths());
    }

    @Test
    void fullIndexConfigAndIncludePathGettersAndSetters() {
        final FullIndexIncludePathConfig includePath = new FullIndexIncludePathConfig();
        includePath.setPath("/content/wknd");
        includePath.setIncludeChildPath(true);

        final FullIndexConfig config = new FullIndexConfig();
        config.setRootPath("/content/wknd");
        config.setIncludePaths(java.util.Collections.singletonList(includePath));
        config.setExcludePaths(new String[] {"/content/x"});

        assertEquals("/content/wknd", config.getRootPath());
        assertEquals(1, config.getIncludePaths().size());
        assertArrayEquals(new String[] {"/content/x"}, config.getExcludePaths());
    }

    @Test
    void languageMappingConfigGettersAndSetters() {
        final LanguageMappingConfig config = new LanguageMappingConfig();
        config.setAemLanguage("en");
        config.setSearchStaxLanguage("abc");
        config.setEnabled(true);

        assertEquals("en", config.getAemLanguage());
        assertTrue(config.isEnabled());
    }

    @Test
    void initialSetupConfigAllowsNullArrays() {
        final InitialSetupConfig config = new InitialSetupConfig();
        config.setRootPaths(null);
        assertNull(config.getRootPaths());
    }

    @Test
    void initialSetupConfigReturnsNullForNullExcludeAndAllowedFiles() {
        final InitialSetupConfig config = new InitialSetupConfig();
        config.setExcludePaths(null);
        config.setAllowedFiles(null);

        assertNull(config.getExcludePaths());
        assertNull(config.getAllowedFiles());
    }

    @Test
    void fullIndexProgressDefaultsNullFieldsToEmptyStrings() {
        final FullIndexProgress progress = new FullIndexProgress(
                FullIndexProgress.State.IDLE,
                0L,
                0L,
                0L,
                0L,
                0L,
                0,
                null,
                0L,
                0L,
                null);

        assertEquals("", progress.getLastIndexedPath());
        assertEquals("", progress.getMessage());
    }
}
