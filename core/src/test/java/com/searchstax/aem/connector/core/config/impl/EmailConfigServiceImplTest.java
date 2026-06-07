package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.config.model.EmailConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailConfigServiceImplTest {

    @Test
    void parsesCommaSeparatedReceiverEmails() {
        assertArrayEquals(
                new String[] {"ops@example.com", "dev@example.com"},
                EmailConfigServiceImpl.parseReceiverEmails("ops@example.com, dev@example.com"));
    }

    @Test
    void returnsEmptyArrayForBlankReceivers() {
        assertEquals(0, EmailConfigServiceImpl.parseReceiverEmails("  ").length);
    }

    @Test
    void returnsNoRecipientsWhenFailureNotificationsDisabled() {
        final EmailConfigServiceImpl service = new EmailConfigServiceImpl() {
            @Override
            public EmailConfig getConfiguration() {
                final EmailConfig config = new EmailConfig();
                config.setNotifyOnIndexingFailure(false);
                config.setReceiverEmails("ops@example.com");
                return config;
            }
        };

        assertEquals(0, service.getReceiverAddresses().length);
    }

    @Test
    void returnsConfiguredRecipientsWhenNotificationsEnabled() {
        final EmailConfigServiceImpl service = new EmailConfigServiceImpl() {
            @Override
            public EmailConfig getConfiguration() {
                final EmailConfig config = new EmailConfig();
                config.setNotifyOnIndexingFailure(true);
                config.setReceiverEmails("ops@example.com, dev@example.com");
                return config;
            }
        };

        assertArrayEquals(
                new String[] {"ops@example.com", "dev@example.com"},
                service.getReceiverAddresses());
    }
}
