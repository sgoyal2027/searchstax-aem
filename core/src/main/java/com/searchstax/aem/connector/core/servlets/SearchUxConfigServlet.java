package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.search.SearchStaxPublicBindingPaths;
import com.searchstax.aem.connector.core.config.search.SearchUxConfigService;
import com.searchstax.aem.connector.core.config.search.SearchUxPublicConfig;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component(
        service = Servlet.class,
        property = {
            Constants.SERVICE_DESCRIPTION + "=SearchStax public search UX configuration",
            ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
            ServletResolverConstants.SLING_SERVLET_PATHS + "=" + SearchStaxPublicBindingPaths.SERVLET_SEARCH_CONFIG,
            Constants.SERVICE_RANKING + ":Integer=200000"
        })
public class SearchUxConfigServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(SearchUxConfigServlet.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Reference
    private transient SearchUxConfigService searchUxConfigService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        try {
            final SearchUxPublicConfig config = searchUxConfigService.getPublicConfig(request);
            final Map<String, Object> body = new LinkedHashMap<>();
            body.put("enabled", config.isEnabled());
            body.put("message", config.getMessage());
            body.put("language", config.getLanguage());
            body.put("searchURL", config.getSearchUrl());
            body.put("suggesterURL", config.getSuggesterUrl());
            body.put("searchAuth", config.getSearchAuth());
            body.put("authType", config.getAuthType());
            body.put("trackApiKey", config.getTrackApiKey());
            body.put("relatedSearchesURL", config.getRelatedSearchesUrl());
            body.put("relatedSearchesAPIKey", config.getRelatedSearchesApiKey());
            body.put("analyticsBaseUrl", config.getAnalyticsBaseUrl());
            body.put("forwardGeocodingEndpoint", config.getForwardGeocodingEndpoint());
            body.put("reverseGeocodingEndpoint", config.getReverseGeocodingEndpoint());

            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json");
            response.setStatus(SlingHttpServletResponse.SC_OK);
            OBJECT_MAPPER.writeValue(response.getWriter(), body);
        } catch (Exception e) {
            LOG.error("Error loading SearchStax UX configuration", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"enabled\":false,\"message\":\"Failed to load search configuration.\"}");
        }
    }
}
