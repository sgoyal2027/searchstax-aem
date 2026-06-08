package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.services.EmailService;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class EmailConfigTestServletTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private EmailConfigService emailConfigService;

    @Mock
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        context.registerService(EmailConfigService.class, emailConfigService);
        context.registerService(EmailService.class, emailService);
    }

    @Test
    void rejectsWhenNoReceiverAddressesConfigured() throws Exception {
        when(emailConfigService.getReceiverAddresses()).thenReturn(new String[0]);

        final EmailConfigTestServlet servlet = context.registerInjectActivateService(new EmailConfigTestServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    void sendsTestEmailWhenConfigured() throws Exception {
        when(emailConfigService.getReceiverAddresses()).thenReturn(new String[] {"ops@example.com"});
        when(emailService.sendEmail(any(EmailRequest.class))).thenReturn(true);

        final EmailConfigTestServlet servlet = context.registerInjectActivateService(new EmailConfigTestServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("Test email sent"));
    }

    @Test
    void returns500WhenEmailSendFails() throws Exception {
        when(emailConfigService.getReceiverAddresses()).thenReturn(new String[] {"ops@example.com"});
        when(emailService.sendEmail(any(EmailRequest.class))).thenReturn(false);

        final EmailConfigTestServlet servlet = context.registerInjectActivateService(new EmailConfigTestServlet());
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, context.response().getStatus());
    }
}
