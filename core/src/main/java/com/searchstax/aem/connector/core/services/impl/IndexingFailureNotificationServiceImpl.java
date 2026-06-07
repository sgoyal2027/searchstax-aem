package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.dto.response.IndexingBatchResult;
import com.searchstax.aem.connector.core.incremental.IndexingAction;
import com.searchstax.aem.connector.core.services.EmailService;
import com.searchstax.aem.connector.core.services.IndexingFailureNotificationService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component(service = IndexingFailureNotificationService.class)
public class IndexingFailureNotificationServiceImpl implements IndexingFailureNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingFailureNotificationServiceImpl.class);

    private static final int MAX_PATHS_IN_EMAIL = 10;

    @Reference
    private EmailConfigService emailConfigService;

    @Reference
    private EmailService emailService;

    @Override
    public void notifyBatchFailure(
            final String batchId,
            final IndexingAction action,
            final IndexingBatchResult result,
            final List<String> paths) {

        final String[] recipients = emailConfigService.getReceiverAddresses();
        if (recipients == null || recipients.length == 0) {
            LOG.info("{} Failure email skipped: notifications disabled or no recipients configured",
                    IncrementalIndexingDefaults.LOG_PREFIX);
            return;
        }

        final StringBuilder body = new StringBuilder();
        body.append("<p>SearchStax incremental indexing failed.</p>");
        body.append("<ul>");
        body.append("<li><strong>Batch ID:</strong> ").append(batchId).append("</li>");
        body.append("<li><strong>Action:</strong> ").append(action).append("</li>");
        body.append("<li><strong>HTTP Status:</strong> ").append(result.getStatusCode()).append("</li>");
        body.append("<li><strong>Item Count:</strong> ").append(result.getItemCount()).append("</li>");
        body.append("<li><strong>Message:</strong> ").append(escape(result.getMessage())).append("</li>");
        body.append("</ul>");

        if (paths != null && !paths.isEmpty()) {
            body.append("<p><strong>Affected paths:</strong></p><ul>");
            final int limit = Math.min(paths.size(), MAX_PATHS_IN_EMAIL);
            for (int index = 0; index < limit; index++) {
                body.append("<li>").append(escape(paths.get(index))).append("</li>");
            }
            if (paths.size() > MAX_PATHS_IN_EMAIL) {
                body.append("<li>... and ").append(paths.size() - MAX_PATHS_IN_EMAIL).append(" more</li>");
            }
            body.append("</ul>");
        }

        final EmailRequest request = new EmailRequest();
        request.setSubject("SearchStax incremental indexing failure - " + action.name());
        request.setBody(body.toString());
        request.setRecipients(recipients);

        final boolean sent = emailService.sendEmail(request);
        if (sent) {
            LOG.info("{} Failure notification email sent for batch {}", IncrementalIndexingDefaults.LOG_PREFIX, batchId);
        } else {
            LOG.warn("{} Failure notification email was not sent for batch {}",
                    IncrementalIndexingDefaults.LOG_PREFIX, batchId);
        }
    }

    private String escape(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
