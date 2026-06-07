package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.constants.SearchStaxServiceLimits;
import com.searchstax.aem.connector.core.dto.response.ApiResponse;
import com.searchstax.aem.connector.core.services.SearchStaxApiErrorPolicy;
import com.searchstax.aem.connector.core.services.SearchStaxConfigurationService;
import com.searchstax.aem.connector.core.services.SearchstaxClientService;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component(service = SearchstaxClientService.class)
public class SearchstaxClientServiceImpl implements SearchstaxClientService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchstaxClientServiceImpl.class);

    private static final int CONNECT_TIMEOUT_MS = 30_000;

    private static final int READ_TIMEOUT_MS = 30_000;

    @Reference
    private SearchStaxConfigurationService configurationService;

    @Reference
    private ProtectedValueCodec protectedValueCodec;

    @Override
    public ApiResponse indexDocument(final String requestJson) {
        return executeRequest(requestJson);
    }

    @Override
    public ApiResponse deleteDocument(final String deleteJson) {
        return executeRequest(deleteJson);
    }

    private ApiResponse executeRequest(final String requestJson) {
        HttpURLConnection connection = null;

        final String baseEndpoint = configurationService.getUpdateEndpoint();
        final String token = configurationService.getUpdateToken();

        if (baseEndpoint == null || baseEndpoint.isBlank()) {
            return new ApiResponse(
                    HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "SearchStax update endpoint is not configured");
        }

        if (token == null || token.isBlank()) {
            return new ApiResponse(
                    HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "SearchStax update token is not configured");
        }

        if (protectedValueCodec != null && protectedValueCodec.looksEncrypted(token)) {
            LOG.error(
                    "{} Update API token is still encrypted after decryption. "
                            + "Re-save API configuration or verify AEM CryptoSupport.",
                    IncrementalIndexingDefaults.LOG_PREFIX);
            return new ApiResponse(
                    HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "Update API token could not be decrypted. Re-save API configuration in the wizard.");
        }

        LOG.info(
                "{} Calling SearchStax update API endpoint={} tokenDecrypted=true",
                IncrementalIndexingDefaults.LOG_PREFIX,
                baseEndpoint);

        try {
            final StringBuilder urlBuilder = new StringBuilder(baseEndpoint);
            if (baseEndpoint.contains("?")) {
                urlBuilder.append('&');
            } else {
                urlBuilder.append('?');
            }
            urlBuilder.append("commitWithin=").append(SearchStaxFullIndexDefaults.COMMIT_WITHIN_MS);

            final String requestUrl = urlBuilder.toString();
            if (requestUrl.length() > SearchStaxServiceLimits.MAX_URL_LENGTH) {
                final String message = SearchStaxApiErrorPolicy.resolveGuidanceMessage(414, null);
                LOG.error("{} {}", IncrementalIndexingDefaults.LOG_PREFIX, message);
                return new ApiResponse(414, message);
            }

            final URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Authorization", "Token " + token);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(requestJson.getBytes(StandardCharsets.UTF_8));
            }

            final int responseCode = connection.getResponseCode();
            final InputStream responseStream =
                    responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            final String response = readResponse(responseStream);

            LOG.debug("SearchStax incremental response code: {}", responseCode);

            return new ApiResponse(responseCode, response);
        } catch (Exception e) {
            LOG.error("Error while calling SearchStax API", e);
            return new ApiResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponse(final InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        final StringBuilder response = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}
