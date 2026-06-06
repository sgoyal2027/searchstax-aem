package com.searchstax.aem.connector.core.config.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.LanguageConfigService;
import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component(service = LanguageConfigService.class)
public class LanguageConfigServiceImpl implements LanguageConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageConfigServiceImpl.class);

    public static final String CONFIG_PATH = "/conf/searchstaxconnector/settings/languagemapping";

    private static final String PROPERTY_NAME = "languageMappings";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Reference
    private ResolverUtil resolverUtil;

    @Override
    public List<LanguageMappingConfig> getLanguageMappings() {
        try (ResourceResolver resourceResolver = resolverUtil.getServiceResolver()) {
            final Resource configResource = resourceResolver.getResource(CONFIG_PATH);
            if (configResource == null) {
                LOG.warn("Language mapping configuration not found at {}", CONFIG_PATH);
                return Collections.emptyList();
            }

            final String mappingsJson = configResource.getValueMap().get(PROPERTY_NAME, String.class);
            if (mappingsJson == null || mappingsJson.trim().isEmpty()) {
                return Collections.emptyList();
            }

            final List<LanguageMappingConfig> mappings = objectMapper.readValue(
                    mappingsJson,
                    new TypeReference<List<LanguageMappingConfig>>() {
                    });

            LOG.debug("Loaded {} language mappings", mappings.size());
            return mappings;
        } catch (Exception e) {
            LOG.error("Error while loading language mappings", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<String> mapToSearchStaxLanguage(final String aemLanguage) {
        if (aemLanguage == null || aemLanguage.trim().isEmpty()) {
            return Optional.empty();
        }

        final String normalizedAemLanguage = normalizeLanguageCode(aemLanguage);

        for (final LanguageMappingConfig mapping : getLanguageMappings()) {
            if (!mapping.isEnabled()) {
                continue;
            }

            if (normalizedAemLanguage.equals(normalizeLanguageCode(mapping.getAemLanguage()))) {
                return Optional.ofNullable(mapping.getSearchStaxLanguage())
                        .map(String::trim)
                        .filter(value -> !value.isEmpty());
            }
        }

        return Optional.empty();
    }

    static String normalizeLanguageCode(final String languageCode) {
        if (languageCode == null) {
            return "";
        }
        return languageCode.trim().replace('-', '_').toLowerCase();
    }
}
