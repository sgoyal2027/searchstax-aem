package com.searchstax.aem.connector.core.utils;

import com.searchstax.aem.connector.core.config.model.EmailConfig;
import org.junit.jupiter.api.Test;

import javax.mail.MessagingException;
import javax.mail.Session;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmtpMailSessionFactoryTest {

    @Test
    void buildPropertiesEnablesStartTlsForPort587() {
        final EmailConfig config = gmailConfig();
        config.setSmtpPort(587);
        config.setSmtpUseSsl(false);
        config.setSmtpUseStartTls(true);
        config.setSmtpRequireStartTls(true);

        final Properties props = SmtpMailSessionFactory.buildProperties(config, true);

        assertEquals("smtp.gmail.com", props.get("mail.smtp.host"));
        assertEquals("587", props.get("mail.smtp.port"));
        assertEquals("true", props.get("mail.smtp.starttls.enable"));
        assertEquals("true", props.get("mail.smtp.starttls.required"));
        assertEquals("false", props.get("mail.smtp.ssl.enable"));
    }

    @Test
    void buildPropertiesEnablesSslForPort465() {
        final EmailConfig config = gmailConfig();
        config.setSmtpPort(465);
        config.setSmtpUseSsl(true);
        config.setSmtpUseStartTls(false);

        final Properties props = SmtpMailSessionFactory.buildProperties(config, true);

        assertEquals("true", props.get("mail.smtp.ssl.enable"));
        assertEquals("false", props.get("mail.smtp.starttls.enable"));
        assertEquals("465", props.get("mail.smtp.socketFactory.port"));
    }

    @Test
    void applyJava11TlsCompatibilitySetsStartTlsFactoryOnly() throws MessagingException {
        final Properties props = new Properties();

        SmtpMailSessionFactory.applyJava11TlsCompatibility(props, false, true);

        assertNotNull(props.get("mail.smtp.ssl.socketFactory"));
        assertNull(props.get("mail.smtp.socketFactory"));
    }

    @Test
    void applyJava11TlsCompatibilitySetsImplicitSslFactories() throws MessagingException {
        final Properties props = new Properties();

        SmtpMailSessionFactory.applyJava11TlsCompatibility(props, true, false);

        assertNotNull(props.get("mail.smtp.ssl.socketFactory"));
        assertNotNull(props.get("mail.smtp.socketFactory"));
    }

    @Test
    void createSessionReturnsConfiguredMailSession() throws MessagingException {
        final EmailConfig config = gmailConfig();
        config.setSmtpPort(587);
        config.setSmtpUseStartTls(true);

        final Session session = SmtpMailSessionFactory.createSession(config, "app-password", true);

        assertNotNull(session);
        assertEquals("smtp.gmail.com", session.getProperty("mail.smtp.host"));
        assertTrue(session.getProperty("mail.smtp.starttls.enable").contains("true"));
        assertNotNull(session.getProperties().get("mail.smtp.ssl.socketFactory"));
    }

    private static EmailConfig gmailConfig() {
        final EmailConfig config = new EmailConfig();
        config.setSmtpHost("smtp.gmail.com");
        config.setSmtpUser("user@gmail.com");
        return config;
    }
}
