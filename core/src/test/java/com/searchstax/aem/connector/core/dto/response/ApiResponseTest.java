package com.searchstax.aem.connector.core.dto.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiResponseTest {

    @Test
    void exposesStatusCodeAndResponseBody() {
        final ApiResponse response = new ApiResponse(200, "{\"ok\":true}");

        assertEquals(200, response.getStatusCode());
        assertEquals("{\"ok\":true}", response.getResponseBody());
    }
}
