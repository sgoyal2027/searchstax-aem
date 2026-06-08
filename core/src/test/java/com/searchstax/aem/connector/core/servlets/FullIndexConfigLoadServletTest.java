package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.config.FullIndexConfigService;
import com.searchstax.aem.connector.core.config.model.FullIndexConfig;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class FullIndexConfigLoadServletTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private FullIndexConfigService fullIndexConfigService;

    @Mock
    private SlingHttpServletResponse response;

    @Test
    void returnsInternalErrorWhenResponseWriterFails() throws Exception {
        when(fullIndexConfigService.getConfiguration()).thenReturn(new FullIndexConfig());
        when(response.getWriter())
                .thenThrow(new IOException("writer unavailable"))
                .thenReturn(new PrintWriter(new java.io.StringWriter()));

        final FullIndexConfigLoadServlet servlet = new FullIndexConfigLoadServlet();
        TestReflection.inject(servlet, "configService", fullIndexConfigService);
        servlet.doGet(context.request(), response);

        org.mockito.Mockito.verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
