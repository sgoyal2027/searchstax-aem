package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.LoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class EmailConfigServiceImplIntegrationTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    private EmailConfigServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
        service = new EmailConfigServiceImpl();
        TestReflection.inject(service, "resolverUtil", resolverUtil);
    }

    @Test
    void loadsAllEmailPropertiesFromJcr() {
        context.create().resource(
                EmailConfigServiceImpl.CONFIG_PATH,
                "smtpHost", "smtp.gmail.com",
                "smtpPort", 587,
                "smtpUser", "user@example.com",
                "smtpPassword", "secret",
                "fromEmail", "from@example.com",
                "receiverEmails", "ops@example.com",
                "smtpUseSSL", true,
                "smtpUseStartTLS", true,
                "smtpRequireStartTLS", true,
                "debugEmail", true,
                "oauthFlow", true,
                "notifyOnIndexingFailure", false);

        final EmailConfig config = service.getConfiguration();

        assertEquals("smtp.gmail.com", config.getSmtpHost());
        assertEquals(587, config.getSmtpPort());
        assertTrue(config.isSmtpUseSsl());
        assertFalse(config.isNotifyOnIndexingFailure());
    }

    @Test
    void returnsReceiverAddressesWhenNotificationsEnabled() {
        context.create().resource(
                EmailConfigServiceImpl.CONFIG_PATH,
                "notifyOnIndexingFailure", true,
                "receiverEmails", "ops@example.com, dev@example.com");

        assertArrayEquals(
                new String[] {"ops@example.com", "dev@example.com"},
                service.getReceiverAddresses());
    }

    @Test
    void returnsDefaultConfigWhenResourceMissing() {
        assertNull(service.getConfiguration().getSmtpHost());
    }

    @Test
    void returnsEmptyConfigWhenLoginFails() throws Exception {
        when(resolverUtil.getServiceResolver()).thenThrow(new LoginException("denied"));

        assertNull(service.getConfiguration().getSmtpHost());
    }
}
