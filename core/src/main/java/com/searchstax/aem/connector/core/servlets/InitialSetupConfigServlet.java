package com.searchstax.aem.connector.core.servlets;

import com.searchstax.aem.connector.core.utils.ConfigResourceUtil;
import com.searchstax.aem.connector.core.utils.JsonServletResponseUtil;
import com.searchstax.aem.connector.core.utils.MultifieldParseHelper;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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
                Constants.SERVICE_DESCRIPTION + "=SearchStax Initial Setup Config Servlet",
                "sling.servlet.methods=POST",
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/initial-setup-config"
        }
)
public class InitialSetupConfigServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(InitialSetupConfigServlet.class);

    private static final String CONFIG_PATH = "/conf/searchstaxconnector/settings/initialsetupconfig";

    @Reference
    private transient ResolverUtil resolverUtil;

    @Override
    protected void doPost(
            final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) throws ServletException, IOException {

        LOG.info("Initial setup configuration request started");

        final String enableConnectorValue = MultifieldParseHelper.trimToEmpty(request.getParameter("./enableConnector"));
        final boolean enableConnector =
                "true".equalsIgnoreCase(enableConnectorValue)
                        || "on".equalsIgnoreCase(enableConnectorValue);

        final String[] rootPaths = MultifieldParseHelper.getStringArrayParameter(request, "rootPaths");
        final String[] excludePaths = MultifieldParseHelper.getStringArrayParameter(request, "excludePaths");
        final String[] allowedFiles = MultifieldParseHelper.getStringArrayParameter(request, "allowedFiles");

        if (rootPaths.length == 0) {
            JsonServletResponseUtil.writeBadRequest(response, "At least one root path is required.");
            return;
        }

        for (final String rootPath : rootPaths) {
            if (rootPath.trim().isEmpty()) {
                JsonServletResponseUtil.writeBadRequest(response, "Root path cannot be empty.");
                return;
            }
        }

        for (final String excludePath : excludePaths) {
            if (excludePath.trim().isEmpty()) {
                continue;
            }

            boolean valid = false;
            for (final String rootPath : rootPaths) {
                if (excludePath.startsWith(rootPath)) {
                    valid = true;
                    break;
                }
            }

            if (!valid) {
                JsonServletResponseUtil.writeBadRequest(
                        response,
                        "Exclude path must be under one of the configured root paths.");
                return;
            }
        }

        try (ResourceResolver resolver = resolverUtil.getServiceResolver()) {
            final Resource resource = ConfigResourceUtil.getOrCreateConfigResource(resolver, CONFIG_PATH);
            if (resource == null) {
                LOG.error("Configuration resource not found at {}", CONFIG_PATH);
                JsonServletResponseUtil.writeInternalError(response, "Configuration path not found.");
                return;
            }

            final ModifiableValueMap properties = ConfigResourceUtil.getModifiableProperties(resource);
            if (properties == null) {
                JsonServletResponseUtil.writeInternalError(response, "Unable to update configuration.");
                return;
            }

            properties.put("enableConnector", enableConnector);
            properties.put("rootPaths", rootPaths);

            if (excludePaths.length > 0) {
                properties.put("excludePaths", excludePaths);
            } else {
                properties.remove("excludePaths");
            }

            if (allowedFiles.length > 0) {
                properties.put("allowedFiles", allowedFiles);
            } else {
                properties.remove("allowedFiles");
            }

            resolver.commit();

            LOG.info("Initial setup configuration saved successfully at {}", CONFIG_PATH);
            JsonServletResponseUtil.writeSuccess(response, "Initial configuration saved successfully.");
        } catch (LoginException e) {
            LOG.error("Service user login failed while saving initial setup configuration", e);
            JsonServletResponseUtil.writeInternalError(
                    response,
                    "Unable to access configuration storage. Verify service user mapping is installed.");
        } catch (PersistenceException e) {
            LOG.error("Persistence error while saving configuration", e);
            JsonServletResponseUtil.writeInternalError(response, "Unable to save configuration.");
        } catch (Exception e) {
            LOG.error("Unexpected error while saving configuration", e);
            JsonServletResponseUtil.writeInternalError(response, "Unexpected error occurred.");
        }
    }
}
