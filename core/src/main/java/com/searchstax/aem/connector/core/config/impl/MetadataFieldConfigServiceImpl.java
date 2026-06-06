package com.searchstax.aem.connector.core.config.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.config.MetadataFieldConfigService;
import com.searchstax.aem.connector.core.config.model.MetadataFieldMappingConfig;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@Component(service = MetadataFieldConfigService.class)
public class MetadataFieldConfigServiceImpl implements MetadataFieldConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataFieldConfigServiceImpl.class);

    private static final String CONFIG_PATH =
            "/conf/searchstaxconnector/settings/metadatafieldmapping";

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Reference
    private ResolverUtil resolverUtil;

    @Override
    public List<MetadataFieldMappingConfig> getMetadataFieldMappings() {

        LOG.info("Loading metadata field mappings from configuration");

        try (ResourceResolver resourceResolver =
                     resolverUtil.getServiceResolver()) {

            Resource configResource =
                    resourceResolver.getResource(CONFIG_PATH);

            LOG.info(
                "Config path: {}",
                CONFIG_PATH);

                LOG.info(
                "Config resource found: {}",
                configResource != null);

            if (configResource == null) {

                LOG.warn(
                        "Configuration resource not found at path: {}",
                        CONFIG_PATH);

                return Collections.emptyList();
            }

            String mappingsJson =
                    configResource.getValueMap().get(
                            "metadataMappings",
                            String.class);

                LOG.info(
        "Metadata mappings JSON: {}",
        mappingsJson);

            if (mappingsJson == null
                    || mappingsJson.trim().isEmpty()) {

                LOG.warn(
                        "No metadata field mappings found in configuration");

                return Collections.emptyList();
            }

            List<MetadataFieldMappingConfig> mappings =
                    objectMapper.readValue(
                            mappingsJson,
                            new TypeReference<List<MetadataFieldMappingConfig>>() {
                            });

            LOG.info(
                    "Successfully loaded {} metadata field mappings",
                    mappings.size());

            for (MetadataFieldMappingConfig mapping : mappings) {

                LOG.debug(
                        "Mapping loaded: aemField={}, customProperty={}, searchStaxField={}, type={}, enabled={}",
                        mapping.getAemField(),
                        mapping.getCustomProperty(),
                        mapping.getSearchStaxField(),
                        mapping.getType(),
                        mapping.isEnabled());
            }

            return mappings;

        } catch (Exception e) {

            LOG.error(
                    "Error while loading metadata field mappings",
                    e);

            return Collections.emptyList();
        }
    }
}
