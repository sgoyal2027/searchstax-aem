package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.services.EmailService;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private EmailConfigService emailConfigService;

    @Mock
    private ProtectedValueCodec protectedValueCodec;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl();
        inject(emailService, "emailConfigService", emailConfigService);
        inject(emailService, "protectedValueCodec", protectedValueCodec);
    }

    @Test
    void skipsWhenRecipientsMissing() {
        final EmailRequest request = new EmailRequest();
        request.setSubject("Subject");
        request.setBody("Body");
        request.setRecipients(new String[0]);

        assertFalse(emailService.sendEmail(request));
    }

    @Test
    void skipsWhenSmtpHostMissing() {
        when(emailConfigService.getConfiguration()).thenReturn(new EmailConfig());

        final EmailRequest request = requestFor("user@example.com");

        assertFalse(emailService.sendEmail(request));
    }

    @Test
    void skipsWhenRequestIsNull() {
        assertFalse(emailService.sendEmail(null));
    }

    @Test
    void skipsWhenRecipientsAreNull() {
        final EmailRequest request = new EmailRequest();
        request.setSubject("Subject");
        request.setBody("Body");
        request.setRecipients(null);

        assertFalse(emailService.sendEmail(request));
    }

    @Test
    void returnsFalseWhenSmtpSendFails() {
        final EmailConfig config = smtpConfig();
        config.setSmtpPassword("plain-password");
        config.setFromEmail("from@example.com");

        when(emailConfigService.getConfiguration()).thenReturn(config);
        when(protectedValueCodec.unprotectIfNeeded(anyString())).thenReturn("plain-password");
        when(protectedValueCodec.looksEncrypted(anyString())).thenReturn(false);

        assertFalse(emailService.sendEmail(requestFor("user@example.com")));
    }

    @Test
    void appliesStartTlsSettingsForPort587() {
        final EmailConfig config = smtpConfig();
        config.setSmtpPort(587);
        config.setSmtpUseSsl(false);
        config.setSmtpUseStartTls(true);
        config.setSmtpPassword("plain-password");

        when(emailConfigService.getConfiguration()).thenReturn(config);
        when(protectedValueCodec.unprotectIfNeeded(anyString())).thenReturn("plain-password");
        when(protectedValueCodec.looksEncrypted(anyString())).thenReturn(false);

        assertFalse(emailService.sendEmail(requestFor("user@example.com")));
    }

    @Test
    void skipsWhenEncryptedPasswordCannotBeDecrypted() {
        final EmailConfig config = smtpConfig();
        config.setSmtpPassword("{encrypted-password-blob-that-is-still-protected-after-decrypt}");

        when(emailConfigService.getConfiguration()).thenReturn(config);
        when(protectedValueCodec.unprotectIfNeeded(config.getSmtpPassword())).thenReturn("{still-encrypted}");
        when(protectedValueCodec.looksEncrypted("{still-encrypted}")).thenReturn(true);

        assertFalse(emailService.sendEmail(requestFor("user@example.com")));
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
        config.setSmtpHost("smtp.gmail.com");
        config.setSmtpPort(465);
        config.setSmtpUser("user@example.com");
        config.setSmtpUseSsl(true);
        return config;
    }

    private static void inject(final Object target, final String fieldName, final Object value) {
        try {
            final java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
