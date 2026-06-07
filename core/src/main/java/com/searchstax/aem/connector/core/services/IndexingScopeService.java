package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.incremental.IndexingScopeDecision;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public interface IndexingScopeService {

    IndexingScopeDecision evaluate(ResourceResolver resolver, String path);

    boolean isConnectorEnabled();

    Resource resolveIndexableResource(ResourceResolver resolver, String path);
}
