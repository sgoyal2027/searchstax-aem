package com.searchstax.aem.connector.core.services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Optional;

public interface SearchStaxDocumentBuilderService {

    Optional<ObjectNode> buildDocument(ResourceResolver resolver, String path);

    String resolveDocumentId(String path);

    Optional<ObjectNode> buildDeletePayload(String path);
}
