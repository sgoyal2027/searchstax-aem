package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceImplExtendedTest {

    @Mock
    private EmailConfigService emailConfigService;

    @Mock
    private ProtectedValueCodec protectedValueCodec;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl();
        TestReflection.inject(emailService, "emailConfigService", emailConfigService);
        TestReflection.inject(emailService, "protectedValueCodec", protectedValueCodec);
    }

    @AfterEach
    void tearDown() {
        // no-op
    }

    @Test
    void appliesPlainSmtpSecurityWhenSslAndStartTlsDisabled() throws Exception {
        final HtmlEmail email = new HtmlEmail();
        final EmailConfig config = smtpConfig();
        config.setSmtpPort(25);
        config.setSmtpUseSsl(false);
        config.setSmtpUseStartTls(false);

        invokeApplySmtpSecurity(email, config);

        assertFalse(email.isSSL());
        assertFalse(email.isStartTLSEnabled());
        assertFalse(email.isStartTLSRequired());
    }

    @Test
    void resolveEmailErrorMessageMapsGmailAppPasswordHint() throws Exception {
        final EmailException exception = new EmailException("Application-specific password required");

        assertTrue(invokeResolveEmailErrorMessage(exception).contains("Google App Password"));
    }

    @Test
    void resolveEmailErrorMessageMapsAuthenticationFailures() throws Exception {
        final EmailException exception = new EmailException("AuthenticationFailedException: 535");

        assertTrue(invokeResolveEmailErrorMessage(exception).contains("SMTP authentication failed"));
    }

    private EmailConfig smtpConfig() {
        final EmailConfig config = new EmailConfig();
        config.setSmtpHost("smtp.example.com");
        config.setSmtpPort(465);
        config.setSmtpUser("user@example.com");
        config.setSmtpPassword("password");
        config.setSmtpUseSsl(true);
        return config;
    }

    private void invokeApplySmtpSecurity(final HtmlEmail email, final EmailConfig config) throws Exception {
        final Method method = EmailServiceImpl.class.getDeclaredMethod("applySmtpSecurity", HtmlEmail.class, EmailConfig.class);
        method.setAccessible(true);
        method.invoke(emailService, email, config);
    }

    private String invokeResolveEmailErrorMessage(final EmailException exception) throws Exception {
        final Method method = EmailServiceImpl.class.getDeclaredMethod("resolveEmailErrorMessage", EmailException.class);
        method.setAccessible(true);
        return (String) method.invoke(emailService, exception);
    }

}
