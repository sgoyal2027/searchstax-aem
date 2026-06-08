package com.searchstax.aem.connector.core.dto.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmailRequestTest {

    @Test
    void gettersAndSettersRoundTripValues() {
        final EmailRequest request = new EmailRequest();
        request.setSubject("Subject");
        request.setBody("Body");
        request.setRecipients(new String[] {"a@example.com", "b@example.com"});

        assertEquals("Subject", request.getSubject());
        assertEquals("Body", request.getBody());
        assertArrayEquals(new String[] {"a@example.com", "b@example.com"}, request.getRecipients());
    }

    @Test
    void returnsDefensiveCopyOfRecipients() {
        final EmailRequest request = new EmailRequest();
        final String[] recipients = new String[] {"a@example.com"};
        request.setRecipients(recipients);

        recipients[0] = "changed@example.com";
        final String[] returned = request.getRecipients();

        assertArrayEquals(new String[] {"a@example.com"}, returned);
        assertNotSame(recipients, returned);
    }

    @Test
    void acceptsNullRecipients() {
        final EmailRequest request = new EmailRequest();
        request.setRecipients(null);

        assertNull(request.getRecipients());
    }
}
