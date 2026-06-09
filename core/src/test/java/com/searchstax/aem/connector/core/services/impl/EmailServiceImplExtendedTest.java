package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.mail.MessagingException;
import javax.net.ssl.SSLHandshakeException;
import java.lang.reflect.Method;

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

    @Test
    void resolveEmailErrorMessageMapsGmailAppPasswordHint() throws Exception {
        final MessagingException exception = new MessagingException("Application-specific password required");

        assertTrue(invokeResolveEmailErrorMessage(exception).contains("Google App Password"));
    }

    @Test
    void resolveEmailErrorMessageMapsAuthenticationFailures() throws Exception {
        final MessagingException exception = new MessagingException("AuthenticationFailedException: 535");

        assertTrue(invokeResolveEmailErrorMessage(exception).contains("SMTP authentication failed"));
    }

    @Test
    void resolveEmailErrorMessageMapsTlsHandshakeFailures() throws Exception {
        final MessagingException exception = new MessagingException(
                "Could not convert socket to TLS",
                new SSLHandshakeException("No appropriate protocol"));

        assertTrue(invokeResolveEmailErrorMessage(exception).contains("SMTP TLS handshake failed"));
        assertTrue(invokeResolveEmailErrorMessage(exception).contains("JavaMail"));
    }

    @Test
    void storesLastSendErrorWhenSmtpPasswordCannotBeDecrypted() {
        final EmailConfig config = smtpConfig();
        config.setSmtpPassword("{encrypted}");

        when(emailConfigService.getConfiguration()).thenReturn(config);
        when(protectedValueCodec.unprotectIfNeeded(anyString())).thenReturn("{still-encrypted}");
        when(protectedValueCodec.looksEncrypted("{still-encrypted}")).thenReturn(true);

        assertFalse(emailService.sendEmail(requestFor("user@example.com")));
        assertTrue(emailService.getLastSendError().contains("password"));
    }

    private EmailRequest requestFor(final String recipient) {
        final EmailRequest request = new EmailRequest();
        request.setSubject("Subject");
        request.setBody("Body");
        request.setRecipients(new String[] {recipient});
        return request;
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

    private String invokeResolveEmailErrorMessage(final Exception exception) throws Exception {
        final Method method = EmailServiceImpl.class.getDeclaredMethod("resolveEmailErrorMessage", Exception.class);
        method.setAccessible(true);
        return (String) method.invoke(emailService, exception);
    }
}
