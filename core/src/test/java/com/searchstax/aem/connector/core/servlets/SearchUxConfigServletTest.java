package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.search.SearchUxConfigService;
import com.searchstax.aem.connector.core.config.search.SearchUxPublicConfig;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class SearchUxConfigServletTest {

    private final AemContext context = AppAemContext.newAemContext();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SearchUxConfigService searchUxConfigService;

    @BeforeEach
    void setUp() {
        context.registerService(SearchUxConfigService.class, searchUxConfigService);
    }

    @Test
    void returnsDisabledConfigWhenConnectorOff() throws Exception {
        final SearchUxPublicConfig config = new SearchUxPublicConfig();
        config.setEnabled(false);
        config.setMessage("SearchStax connector is disabled in Initial Setup.");
        when(searchUxConfigService.getPublicConfig(context.request())).thenReturn(config);

        final SearchUxConfigServlet servlet = new SearchUxConfigServlet();
        TestReflection.inject(servlet, "searchUxConfigService", searchUxConfigService);
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertFalse(json.get("enabled").asBoolean());
        assertTrue(json.get("message").asText().contains("disabled"));
    }

    @Test
    void returnsSearchEndpointsFromConnectorConfig() throws Exception {
        final SearchUxPublicConfig config = new SearchUxPublicConfig();
        config.setEnabled(true);
        config.setLanguage("en");
        config.setSearchUrl("https://search.example/emselect");
        config.setSuggesterUrl("https://search.example/emsuggest");
        config.setSearchAuth("token-value");
        config.setAuthType("token");
        when(searchUxConfigService.getPublicConfig(context.request())).thenReturn(config);

        final SearchUxConfigServlet servlet = new SearchUxConfigServlet();
        TestReflection.inject(servlet, "searchUxConfigService", searchUxConfigService);
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertTrue(json.get("enabled").asBoolean());
        assertEquals("en", json.get("language").asText());
        assertEquals("https://search.example/emselect", json.get("searchURL").asText());
        assertEquals("token-value", json.get("searchAuth").asText());
    }
}
