package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.dto.request.EmailRequest;
import com.searchstax.aem.connector.core.services.EmailService;
import com.searchstax.aem.connector.core.utils.JsonServletResponseUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Component(
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=SearchStax Email Config Test Servlet",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST,
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/email-config-test"
        })
public class EmailConfigTestServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(EmailConfigTestServlet.class);

    @Reference
    private transient EmailConfigService emailConfigService;

    @Reference
    private transient EmailService emailService;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws ServletException, IOException {

        LOG.info("Email configuration test request started");

        final String[] recipients = emailConfigService.getReceiverAddresses();
        if (recipients == null || recipients.length == 0) {
            JsonServletResponseUtil.writeBadRequest(
                    response,
                    "Configure at least one receiver email and enable failure notifications.");
            return;
        }

        final EmailRequest emailRequest = new EmailRequest();
        emailRequest.setRecipients(recipients);
        emailRequest.setSubject("SearchStax AEM Connector - test email");
        emailRequest.setBody(
                "<p>This is a test email from the SearchStax AEM Connector.</p>"
                        + "<p>If you received this message, SMTP settings are working.</p>");

        if (emailService.sendEmail(emailRequest)) {
            JsonServletResponseUtil.writeSuccess(
                    response,
                    "Test email sent to " + recipients.length + " recipient(s).");
            return;
        }

        JsonServletResponseUtil.writeInternalError(
                response,
                "Test email failed. Check searchstaxconnector.log for SMTP authentication or TLS details.");
    }
}
