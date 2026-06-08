package com.searchstax.aem.connector.core.utils;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class JsonServletResponseUtilTest {

    private final AemContext context = new AemContext();

    @Test
    void writeSuccessReturnsOkJson() throws Exception {
        final MockSlingHttpServletResponse response = context.response();

        JsonServletResponseUtil.writeSuccess(response, "Saved");

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("{\"success\":true,\"message\":\"Saved\"}", response.getOutputAsString());
    }

    @Test
    void writeBadRequestEscapesQuotesInMessage() throws Exception {
        final MockSlingHttpServletResponse response = context.response();

        JsonServletResponseUtil.writeBadRequest(response, "Invalid \"root\" path");

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        assertEquals(
                "{\"success\":false,\"message\":\"Invalid \\\"root\\\" path\"}",
                response.getOutputAsString());
    }

    @Test
    void writeInternalErrorReturnsServerErrorStatus() throws Exception {
        final MockSlingHttpServletResponse response = context.response();

        JsonServletResponseUtil.writeInternalError(response, "Failed");

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals("{\"success\":false,\"message\":\"Failed\"}", response.getOutputAsString());
    }

    @Test
    void escapeJsonHandlesNullAsEmptyString() {
        assertEquals("", JsonServletResponseUtil.escapeJson(null));
    }
}
