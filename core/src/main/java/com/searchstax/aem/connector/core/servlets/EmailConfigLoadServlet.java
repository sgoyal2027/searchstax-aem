package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchstax.aem.connector.core.config.EmailConfigService;
import com.searchstax.aem.connector.core.config.model.EmailConfig;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;

@Component(
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=SearchStax Email Config Load Servlet",
                "sling.servlet.methods=GET",
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/email-config-load"
        })
public class EmailConfigLoadServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(EmailConfigLoadServlet.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Reference
    private transient EmailConfigService emailConfigService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        try {
            final EmailConfig config = emailConfigService.getConfiguration();
            final ObjectNode json = OBJECT_MAPPER.createObjectNode();

            json.put("smtpHost", nullToEmpty(config.getSmtpHost()));
            json.put("smtpPort", config.getSmtpPort() > 0 ? config.getSmtpPort() : 25);
            json.put("smtpUser", nullToEmpty(config.getSmtpUser()));
            json.put("smtpPassword", "");
            json.put("fromEmail", nullToEmpty(config.getFromEmail()));
            json.put("receiverEmails", nullToEmpty(config.getReceiverEmails()));
            json.put("smtpUseSSL", config.isSmtpUseSsl());
            json.put("smtpUseStartTLS", config.isSmtpUseStartTls());
            json.put("notifyOnIndexingFailure", config.isNotifyOnIndexingFailure());

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print(json.toString());
        } catch (Exception e) {
            LOG.error("Error loading email configuration", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            final ObjectNode errorJson = OBJECT_MAPPER.createObjectNode();
            errorJson.put("error", "Unable to load configuration");
            response.getWriter().print(errorJson.toString());
        }
    }

    private static String nullToEmpty(final String value) {
        return value == null ? "" : value;
    }
}
