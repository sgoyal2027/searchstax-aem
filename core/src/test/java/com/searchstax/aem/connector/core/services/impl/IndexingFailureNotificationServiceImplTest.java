package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.dto.response.IndexingBatchResult;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.services.EmailService;
import com.searchstax.aem.connector.core.services.IndexingFailureNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexingFailureNotificationServiceImplTest {

    @Mock
    private EmailConfigService emailConfigService;

    @Mock
    private EmailService emailService;

    private IndexingFailureNotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new IndexingFailureNotificationServiceImpl();
        inject(notificationService, "emailConfigService", emailConfigService);
        inject(notificationService, "emailService", emailService);
    }

    @Test
    void skipsNotificationWhenNoRecipientsConfigured() {
        when(emailConfigService.getReceiverAddresses()).thenReturn(new String[0]);

        notificationService.notifyBatchFailure(
                "batch-1",
                IndexingAction.INDEX,
                new IndexingBatchResult(false, 400, "Bad request", 1, 10L),
                Arrays.asList("/content/wknd/us/en/page"));

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    void sendsFailureEmailToConfiguredRecipients() {
        when(emailConfigService.getReceiverAddresses()).thenReturn(new String[] {"ops@example.com"});
        when(emailService.sendEmail(any())).thenReturn(true);

        notificationService.notifyBatchFailure(
                "batch-2",
                IndexingAction.INDEX,
                new IndexingBatchResult(false, 400, "Bad request", 2, 25L),
                Arrays.asList("/content/wknd/us/en/page-one", "/content/wknd/us/en/page-two"));

        final ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailService).sendEmail(captor.capture());

        final EmailRequest request = captor.getValue();
        assertEquals("SearchStax incremental indexing failure - INDEX", request.getSubject());
        assertEquals(1, request.getRecipients().length);
        assertEquals("ops@example.com", request.getRecipients()[0]);
        assertTrue(request.getBody().contains("batch-2"));
        assertTrue(request.getBody().contains("/content/wknd/us/en/page-one"));
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
