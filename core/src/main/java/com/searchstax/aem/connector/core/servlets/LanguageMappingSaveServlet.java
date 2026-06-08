package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.impl.LanguageConfigServiceImpl;
import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;
import com.searchstax.aem.connector.core.utils.ConfigResourceUtil;
import com.searchstax.aem.connector.core.utils.MultifieldParseHelper;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import com.searchstax.aem.connector.core.utils.JsonServletResponseUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/language-mappings",
                "sling.servlet.methods=" + HttpConstants.METHOD_POST
        }
)
public class LanguageMappingSaveServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageMappingSaveServlet.class);

    private static final String CONFIG_PATH = LanguageConfigServiceImpl.CONFIG_PATH;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Reference
    private transient ResolverUtil resolverUtil;

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {

        final List<LanguageMappingConfig> mappings = new ArrayList<>();

        for (final Map.Entry<Integer, Map<String, String>> entry
                : MultifieldParseHelper.parseItems(
                        request,
                        "languageMappings",
                        "aemLanguageType",
                        "customAemLanguage",
                        "searchStaxLanguage",
                        "enabled").entrySet()) {

            final Map<String, String> item = entry.getValue();
            final String aemLanguageType = item.get("aemLanguageType");

            if (aemLanguageType == null || aemLanguageType.isBlank()) {
                continue;
            }

            final LanguageMappingConfig config = new LanguageMappingConfig();

            if ("custom".equals(aemLanguageType)) {
                config.setAemLanguage(MultifieldParseHelper.trimToEmpty(item.get("customAemLanguage")));
            } else {
                config.setAemLanguage(aemLanguageType);
            }

            config.setSearchStaxLanguage(MultifieldParseHelper.trimToEmpty(item.get("searchStaxLanguage")));
            config.setEnabled(MultifieldParseHelper.isExplicitlyEnabled(item, "enabled"));

            mappings.add(config);
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

            properties.put("languageMappings", OBJECT_MAPPER.writeValueAsString(mappings));
            resolver.commit();

            LOG.info("Saved {} language mapping(s) at {}", mappings.size(), CONFIG_PATH);
            JsonServletResponseUtil.writeSuccess(response, "Language mappings saved successfully.");
        } catch (PersistenceException e) {
            LOG.error("Persistence error while saving language mappings", e);
            JsonServletResponseUtil.writeInternalError(response, "Unable to save configuration.");
        } catch (Exception e) {
            LOG.error("Unexpected error while saving language mappings", e);
            JsonServletResponseUtil.writeInternalError(response, "Unexpected error occurred.");
        }
    }
}
