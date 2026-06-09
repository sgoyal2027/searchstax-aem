package com.searchstax.aem.connector.core.utils;

import com.searchstax.aem.connector.core.config.model.EmailConfig;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.net.ssl.SSLSocketFactory;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

/**
 * Builds a JavaMail {@link Session} for Java 11+ SMTP (STARTTLS and implicit SSL).
 */
public final class SmtpMailSessionFactory {

    private static final int GMAIL_SSL_PORT = 465;
    private static final int GMAIL_STARTTLS_PORT = 587;
    private static final String TLS_PROTOCOLS = "TLSv1.2 TLSv1.3";

    private SmtpMailSessionFactory() {
    }

    public static Session createSession(
            final EmailConfig config,
            final String smtpPassword,
            final boolean hasCredentials) throws MessagingException {

        final Properties props = buildProperties(config, hasCredentials);
        final int port = config.getSmtpPort();
        final boolean sslOnConnect = isSslOnConnect(config, port);
        final boolean startTls = isStartTls(config, port);

        applyJava11TlsCompatibility(props, sslOnConnect, startTls);

        final Authenticator authenticator = hasCredentials
                ? createAuthenticator(config.getSmtpUser().trim(), smtpPassword)
                : null;

        return Session.getInstance(props, authenticator);
    }

    static Properties buildProperties(final EmailConfig config, final boolean hasCredentials) {
        final Properties props = new Properties();
        final String host = config.getSmtpHost().trim();
        final int port = config.getSmtpPort();
        final boolean sslOnConnect = isSslOnConnect(config, port);
        final boolean startTls = isStartTls(config, port);

        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", Boolean.toString(hasCredentials));
        props.put("mail.smtp.ssl.protocols", TLS_PROTOCOLS);
        props.put("mail.smtps.ssl.protocols", TLS_PROTOCOLS);
        props.put("mail.smtp.localhost", "aem-searchstax-connector");

        if (sslOnConnect) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.starttls.required", "false");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port > 0 ? port : GMAIL_SSL_PORT));
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.ssl.trust", host);
            return props;
        }

        if (startTls) {
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.trust", host);
            return props;
        }

        props.put("mail.smtp.ssl.enable", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.starttls.required", "false");
        return props;
    }

    static void applyJava11TlsCompatibility(
            final Properties props,
            final boolean sslOnConnect,
            final boolean startTls) throws MessagingException {

        if (!sslOnConnect && !startTls) {
            return;
        }

        try {
            final SSLSocketFactory socketFactory = new Tls12SocketFactory();
            props.put("mail.smtp.ssl.socketFactory", socketFactory);
            if (sslOnConnect) {
                props.put("mail.smtp.socketFactory", socketFactory);
            }
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new MessagingException("Unable to initialize TLS socket factory for SMTP", e);
        }
    }

    private static Authenticator createAuthenticator(final String username, final String password) {
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
    }

    private static boolean isSslOnConnect(final EmailConfig config, final int port) {
        final boolean useStartTls = isStartTls(config, port);
        final boolean useSsl = config.isSmtpUseSsl() || port == GMAIL_SSL_PORT;
        return useSsl && !useStartTls;
    }

    private static boolean isStartTls(final EmailConfig config, final int port) {
        return config.isSmtpUseStartTls() || port == GMAIL_STARTTLS_PORT;
    }
}
