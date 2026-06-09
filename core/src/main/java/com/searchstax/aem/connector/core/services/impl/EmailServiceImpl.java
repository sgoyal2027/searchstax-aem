package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.services.EmailService;
import com.searchstax.aem.connector.core.utils.ProtectedValueCodec;
import com.searchstax.aem.connector.core.utils.SmtpMessageSender;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.net.ssl.SSLHandshakeException;

@Component(service = EmailService.class)
public class EmailServiceImpl implements EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Reference
    private EmailConfigService emailConfigService;

    @Reference
    private ProtectedValueCodec protectedValueCodec;

    private volatile String lastSendError;

    @Override
    public boolean sendEmail(final EmailRequest request) {
        lastSendError = null;

        if (request == null || request.getRecipients() == null || request.getRecipients().length == 0) {
            lastSendError = "No email recipients configured.";
            LOG.warn("Email request skipped: no recipients");
            return false;
        }

        final EmailConfig config = emailConfigService.getConfiguration();
        if (!isSmtpConfigured(config)) {
            lastSendError = "SMTP host and port are not configured.";
            LOG.error("Email request skipped: SMTP is not configured in Author UI");
            return false;
        }

        try {
            final String smtpPassword = resolveSmtpPassword(config);
            if (hasSmtpCredentials(config) && smtpPassword.isEmpty()) {
                lastSendError = "SMTP password is missing or could not be decrypted. Re-save Email Configuration.";
                LOG.error("Email request skipped: SMTP password is missing or could not be decrypted");
                return false;
            }

            SmtpMessageSender.send(config, smtpPassword, request);
            LOG.info("Email notification sent successfully to {} recipient(s)", request.getRecipients().length);
            return true;
        } catch (MessagingException e) {
            lastSendError = resolveEmailErrorMessage(e);
            LOG.error("Failed to send email notification: {}", lastSendError, e);
            return false;
        }
    }

    @Override
    public String getLastSendError() {
        return lastSendError;
    }

    private String resolveSmtpPassword(final EmailConfig config) {
        final String decrypted = protectedValueCodec.unprotectIfNeeded(config.getSmtpPassword());
        if (protectedValueCodec.looksEncrypted(decrypted)) {
            LOG.error("SMTP password is still encrypted after decryption. Re-save Email Configuration.");
            return "";
        }
        return decrypted;
    }

    private boolean hasSmtpCredentials(final EmailConfig config) {
        return config.getSmtpUser() != null && !config.getSmtpUser().trim().isEmpty();
    }

    private boolean isSmtpConfigured(final EmailConfig config) {
        return config.getSmtpHost() != null
                && !config.getSmtpHost().trim().isEmpty()
                && config.getSmtpPort() > 0;
    }

    private String resolveEmailErrorMessage(final Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SSLHandshakeException) {
                return tlsFailureMessage();
            }

            final String message = current.getMessage() == null ? "" : current.getMessage();
            if (message.contains("Application-specific password required")) {
                return "Gmail rejected the SMTP login. Create a Google App Password and use it as the SMTP password "
                        + "(https://support.google.com/accounts/answer/185833).";
            }
            if (message.contains("AuthenticationFailedException") || message.contains("535")) {
                return "SMTP authentication failed. Verify SMTP user/password and SSL/TLS settings.";
            }
            if (message.contains("SSLHandshakeException")
                    || message.contains("No appropriate protocol")
                    || message.contains("Could not convert socket to TLS")) {
                return tlsFailureMessage();
            }

            current = current.getCause();
        }

        final String message = exception.getMessage();
        return message == null || message.isEmpty() ? "Unable to send email." : message;
    }

    private String tlsFailureMessage() {
        return "SMTP TLS handshake failed. For Gmail use port 587 with StartTLS enabled (SSL unchecked), "
                + "or port 465 with SSL enabled. Use a Google App Password. Runtime: "
                + SmtpMessageSender.describeJavaMailRuntime();
    }
}
