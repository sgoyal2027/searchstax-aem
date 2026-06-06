package com.searchstax.aem.connector.core.services.impl;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.services.EmailService;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = EmailService.class)
public class EmailServiceImpl implements EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Reference
    private EmailConfigService emailConfigService;

    @Reference
    private CryptoSupport cryptoSupport;

    @Override
    public void sendEmail(final EmailRequest request) {
        if (request == null || request.getRecipients() == null || request.getRecipients().length == 0) {
            LOG.warn("Email request skipped: no recipients");
            return;
        }

        final EmailConfig config = emailConfigService.getConfiguration();
        if (!isSmtpConfigured(config)) {
            LOG.error("Email request skipped: SMTP is not configured in Author UI");
            return;
        }

        try {
            final HtmlEmail email = new HtmlEmail();
            email.setHostName(config.getSmtpHost());
            email.setSmtpPort(config.getSmtpPort());

            if (hasSmtpCredentials(config)) {
                email.setAuthenticator(
                        new DefaultAuthenticator(config.getSmtpUser(), unprotect(config.getSmtpPassword())));
            }

            email.setSSLOnConnect(config.isSmtpUseSsl());
            email.setStartTLSEnabled(config.isSmtpUseStartTls());
            email.setStartTLSRequired(config.isSmtpRequireStartTls());

            if (config.getFromEmail() != null && !config.getFromEmail().trim().isEmpty()) {
                email.setFrom(config.getFromEmail().trim());
            }

            email.setSubject(request.getSubject());
            email.setHtmlMsg(request.getBody());

            for (final String recipient : request.getRecipients()) {
                if (recipient != null && !recipient.trim().isEmpty()) {
                    email.addTo(recipient.trim());
                }
            }

            email.send();
            LOG.info("Email notification sent successfully to {} recipient(s)", request.getRecipients().length);
        } catch (EmailException e) {
            LOG.error("Failed to send email notification", e);
        }
    }

    private boolean hasSmtpCredentials(final EmailConfig config) {
        return config.getSmtpUser() != null && !config.getSmtpUser().trim().isEmpty();
    }

    private boolean isSmtpConfigured(final EmailConfig config) {
        return config.getSmtpHost() != null
                && !config.getSmtpHost().trim().isEmpty()
                && config.getSmtpPort() > 0;
    }

    private String unprotect(final String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (cryptoSupport == null) {
            return value;
        }
        try {
            if (cryptoSupport.isProtected(value)) {
                return cryptoSupport.unprotect(value);
            }
        } catch (CryptoException e) {
            LOG.warn("Failed to decrypt SMTP password; using stored value as-is", e);
        }
        return value;
    }
}
