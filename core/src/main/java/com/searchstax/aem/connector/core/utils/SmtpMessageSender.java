package com.searchstax.aem.connector.core.utils;

import com.searchstax.aem.connector.core.config.model.EmailConfig;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.sun.mail.smtp.SMTPTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Sends SMTP messages with the connector bundle class loader so embedded JavaMail 1.6.2 is used
 * instead of AEM's platform javax.mail 1.5 (which cannot negotiate TLS 1.2 on Java 11).
 */
public final class SmtpMessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(SmtpMessageSender.class);
    private static final ClassLoader MAIL_CLASS_LOADER = SmtpMessageSender.class.getClassLoader();

    private SmtpMessageSender() {
    }

    public static void send(
            final EmailConfig config,
            final String smtpPassword,
            final EmailRequest request) throws MessagingException {

        logJavaMailRuntime(config);

        final boolean hasCredentials = hasSmtpCredentials(config);
        final Session session = SmtpMailSessionFactory.createSession(config, smtpPassword, hasCredentials);
        final MimeMessage message = buildMessage(session, config, request, hasCredentials);

        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(MAIL_CLASS_LOADER);
            sendWithTransport(session, message, config, smtpPassword, hasCredentials);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public static String describeJavaMailRuntime() {
        final Package mailPackage = SMTPTransport.class.getPackage();
        final String version = mailPackage == null ? "unknown" : mailPackage.getImplementationVersion();
        final String location = SMTPTransport.class.getProtectionDomain().getCodeSource().getLocation().toString();
        return "JavaMail " + version + " from " + location;
    }

    private static void sendWithTransport(
            final Session session,
            final MimeMessage message,
            final EmailConfig config,
            final String smtpPassword,
            final boolean hasCredentials) throws MessagingException {

        final Transport transport = session.getTransport("smtp");
        try {
            if (hasCredentials) {
                transport.connect(
                        config.getSmtpHost().trim(),
                        config.getSmtpPort(),
                        config.getSmtpUser().trim(),
                        smtpPassword);
            } else {
                transport.connect();
            }
            transport.sendMessage(message, message.getAllRecipients());
        } finally {
            transport.close();
        }
    }

    private static MimeMessage buildMessage(
            final Session session,
            final EmailConfig config,
            final EmailRequest request,
            final boolean hasCredentials) throws MessagingException {

        final MimeMessage message = new MimeMessage(session);
        message.setFrom(resolveFromAddress(config, hasCredentials));
        message.setRecipients(Message.RecipientType.TO, resolveRecipients(request));
        message.setSubject(request.getSubject(), "UTF-8");
        message.setContent(request.getBody(), "text/html; charset=utf-8");
        message.setSentDate(new Date());
        message.saveChanges();
        return message;
    }

    private static InternetAddress resolveFromAddress(final EmailConfig config, final boolean hasCredentials)
            throws MessagingException {
        if (config.getFromEmail() != null && !config.getFromEmail().trim().isEmpty()) {
            return new InternetAddress(config.getFromEmail().trim());
        }
        if (hasCredentials) {
            return new InternetAddress(config.getSmtpUser().trim());
        }
        throw new MessagingException("From address is required.");
    }

    private static InternetAddress[] resolveRecipients(final EmailRequest request) throws MessagingException {
        final List<InternetAddress> recipients = new ArrayList<>();
        for (final String recipient : request.getRecipients()) {
            if (recipient == null || recipient.trim().isEmpty()) {
                continue;
            }
            recipients.add(new InternetAddress(recipient.trim()));
        }
        if (recipients.isEmpty()) {
            throw new MessagingException("No valid recipients.");
        }
        return recipients.toArray(new InternetAddress[0]);
    }

    private static boolean hasSmtpCredentials(final EmailConfig config) {
        return config.getSmtpUser() != null && !config.getSmtpUser().trim().isEmpty();
    }

    private static void logJavaMailRuntime(final EmailConfig config) {
        LOG.info(
                "SMTP send starting: host={}, port={}, ssl={}, startTls={}, {}",
                config.getSmtpHost(),
                config.getSmtpPort(),
                config.isSmtpUseSsl(),
                config.isSmtpUseStartTls(),
                describeJavaMailRuntime());
    }
}
