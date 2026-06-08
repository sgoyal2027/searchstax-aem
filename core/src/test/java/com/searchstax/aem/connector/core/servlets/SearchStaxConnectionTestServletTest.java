package com.searchstax.aem.connector.core.servlets;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class SearchStaxConnectionTestServletTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private CryptoSupport cryptoSupport;

    private HttpServer httpServer;
    private int serverPort;

    @BeforeEach
    void setUp() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        serverPort = httpServer.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void rejectsMissingParameters() throws Exception {
        final SearchStaxConnectionTestServlet servlet = servletWithCrypto();
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("Missing required parameters"));
    }

    @Test
    void searchUpdateEndpointReturnsSuccessFor200Response() throws Exception {
        startServer(exchange -> {
            writeResponse(exchange, 200, "{}");
        });

        context.request().addRequestParameter("endpointUrl", baseUrl());
        context.request().addRequestParameter("endpointType", "searchUpdate");
        context.request().addRequestParameter("apiToken", "plain-token");

        final SearchStaxConnectionTestServlet servlet = servletWithCrypto();
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("\"success\":true"));
    }

    @Test
    void searchSelectEndpointProbesEmselectPath() throws Exception {
        startServer(exchange -> {
            assertTrue(exchange.getRequestURI().getPath().endsWith("/emselect"));
            writeResponse(exchange, 200, "{}");
        });

        context.request().addRequestParameter("endpointUrl", baseUrl() + "/select");
        context.request().addRequestParameter("endpointType", "searchSelect");
        context.request().addRequestParameter("apiToken", "plain-token");

        final SearchStaxConnectionTestServlet servlet = servletWithCrypto();
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void discoveryEndpointReturnsSuccessFor200Response() throws Exception {
        startServer(exchange -> {
            writeResponse(exchange, 200, "{}");
        });

        context.request().addRequestParameter("endpointUrl", baseUrl() + "/discovery");
        context.request().addRequestParameter("endpointType", "discovery");
        context.request().addRequestParameter("apiToken", "api-key");

        final SearchStaxConnectionTestServlet servlet = servletWithCrypto();
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void rejectsNonHttpEndpointForSearchType() throws Exception {
        context.request().addRequestParameter("endpointUrl", "ftp://invalid.example.com");
        context.request().addRequestParameter("endpointType", "searchUpdate");
        context.request().addRequestParameter("apiToken", "token");

        final SearchStaxConnectionTestServlet servlet = servletWithCrypto();
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void unprotectsProtectedTokenBeforeProbe() throws Exception {
        startServer(exchange -> {
            assertTrue(exchange.getRequestHeaders().getFirst("Authorization").contains("decrypted-token"));
            writeResponse(exchange, 200, "{}");
        });

        when(cryptoSupport.isProtected("{protected}")).thenReturn(true);
        when(cryptoSupport.unprotect("{protected}")).thenReturn("decrypted-token");

        context.request().addRequestParameter("endpointUrl", baseUrl());
        context.request().addRequestParameter("endpointType", "searchUpdate");
        context.request().addRequestParameter("apiToken", "{protected}");

        final SearchStaxConnectionTestServlet servlet = servletWithCrypto();
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void unprotectTokenReturnsEmptyForNullValue() throws Exception {
        final SearchStaxConnectionTestServlet servlet = new SearchStaxConnectionTestServlet();

        final Method unprotect = SearchStaxConnectionTestServlet.class.getDeclaredMethod(
                "unprotectTokenIfNeeded",
                String.class);
        unprotect.setAccessible(true);

        assertEquals("", unprotect.invoke(servlet, (String) null));
    }

    @Test
    void usesRawTokenWhenCryptoSupportUnavailable() throws Exception {
        startServer(exchange -> {
            writeResponse(exchange, 200, "{}");
        });

        context.request().addRequestParameter("endpointUrl", baseUrl());
        context.request().addRequestParameter("endpointType", "searchUpdate");
        context.request().addRequestParameter("apiToken", "raw-token");

        final SearchStaxConnectionTestServlet servlet = new SearchStaxConnectionTestServlet();
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void returnsBadGatewayWhenServerUnreachable() throws Exception {
        context.request().addRequestParameter("endpointUrl", "http://127.0.0.1:1/unreachable");
        context.request().addRequestParameter("endpointType", "searchUpdate");
        context.request().addRequestParameter("apiToken", "token");

        final SearchStaxConnectionTestServlet servlet = servletWithCrypto();
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_GATEWAY, context.response().getStatus());
    }

    @Test
    void returnsBadRequestFor401FromSearchEndpoint() throws Exception {
        startServer(exchange -> writeResponse(exchange, 401, "Unauthorized"));

        context.request().addRequestParameter("endpointUrl", baseUrl());
        context.request().addRequestParameter("endpointType", "general");
        context.request().addRequestParameter("apiToken", "bad-token");

        final SearchStaxConnectionTestServlet servlet = servletWithCrypto();
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("Invalid or unauthorized token"));
    }

    @Test
    void returnsBadRequestFor404FromDiscoveryEndpoint() throws Exception {
        startServer(exchange -> writeResponse(exchange, 404, "Not found"));

        context.request().addRequestParameter("endpointUrl", baseUrl());
        context.request().addRequestParameter("endpointType", "general");
        context.request().addRequestParameter("apiToken", "bad-token");

        final SearchStaxConnectionTestServlet servlet = servletWithCrypto();
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("Incorrect Host URL or context path."));
    }

    @Test
    void fallsBackToRawTokenWhenUnprotectFails() throws Exception {
        startServer(exchange -> writeResponse(exchange, 200, "{}"));

        when(cryptoSupport.isProtected("{protected}")).thenReturn(true);
        when(cryptoSupport.unprotect("{protected}")).thenThrow(new CryptoException("fail"));

        context.request().addRequestParameter("endpointUrl", baseUrl());
        context.request().addRequestParameter("endpointType", "searchUpdate");
        context.request().addRequestParameter("apiToken", "{protected}");

        final SearchStaxConnectionTestServlet servlet = servletWithCrypto();
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
    }

    @Test
    void escapeJsonReturnsEmptyForNullValue() throws Exception {
        final SearchStaxConnectionTestServlet servlet = new SearchStaxConnectionTestServlet();

        final Method escapeJson = SearchStaxConnectionTestServlet.class.getDeclaredMethod(
                "escapeJson",
                String.class);
        escapeJson.setAccessible(true);

        assertEquals("", escapeJson.invoke(servlet, (String) null));
    }

    private SearchStaxConnectionTestServlet servletWithCrypto() {
        final SearchStaxConnectionTestServlet servlet = new SearchStaxConnectionTestServlet();
        TestReflection.inject(servlet, "cryptoSupport", cryptoSupport);
        return servlet;
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + serverPort;
    }

    private void startServer(final HttpHandler handler) {
        httpServer.createContext("/", handler);
        httpServer.start();
    }

    private static void writeResponse(final HttpExchange exchange, final int status, final String body)
            throws IOException {
        exchange.sendResponseHeaders(status, body.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body.getBytes());
        }
    }
}
