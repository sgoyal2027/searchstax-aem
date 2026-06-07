package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.config.InitialSetupConfigService;
import com.searchstax.aem.connector.core.config.model.InitialSetupConfig;
import com.searchstax.aem.connector.core.constants.IncrementalIndexingDefaults;
import com.searchstax.aem.connector.core.incremental.IndexingScopeDecision;
import com.searchstax.aem.connector.core.services.IndexingScopeService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

@Component(service = IndexingScopeService.class)
public class IndexingScopeServiceImpl implements IndexingScopeService {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingScopeServiceImpl.class);

    private static final String NT_PAGE = "cq:Page";
    private static final String NT_PAGE_CONTENT = "cq:PageContent";
    private static final String NT_ASSET = "dam:Asset";
    private static final String NT_ASSET_CONTENT = "dam:AssetContent";
    private static final String DAM_ROOT = "/content/dam";

    @Reference
    private InitialSetupConfigService initialSetupConfigService;

    @Override
    public boolean isConnectorEnabled() {
        final InitialSetupConfig config = initialSetupConfigService.getConfiguration();
        return config != null && config.isEnableConnector();
    }

    @Override
    public IndexingScopeDecision evaluate(final ResourceResolver resolver, final String path) {
        if (!isConnectorEnabled()) {
            return IndexingScopeDecision.reject("connector disabled");
        }

        if (path == null || path.isBlank()) {
            return IndexingScopeDecision.reject("empty path");
        }

        final String normalizedPath = SearchStaxFullIndexPathConfigurationServiceImpl
                .normalizePath(path, false);

        if (normalizedPath.isEmpty()) {
            return IndexingScopeDecision.reject("invalid path");
        }

        final InitialSetupConfig config = initialSetupConfigService.getConfiguration();
        final String[] rootPaths = SearchStaxFullIndexPathConfigurationServiceImpl
                .normalizeAndDedupe(config.getRootPaths(), true);
        final String[] excludePaths = SearchStaxFullIndexPathConfigurationServiceImpl
                .normalizeAndDedupe(config.getExcludePaths(), false);

        if (rootPaths.length == 0) {
            return IndexingScopeDecision.reject("no root paths configured");
        }

        if (!isUnderAnyRoot(normalizedPath, rootPaths)) {
            return IndexingScopeDecision.reject("outside configured root paths");
        }

        for (final String exclude : excludePaths) {
            if (SearchStaxFullIndexPathConfigurationServiceImpl.isPathUnder(normalizedPath, exclude)) {
                return IndexingScopeDecision.reject("path is excluded");
            }
        }

        final Resource resource = resolver.getResource(normalizedPath);
        if (resource == null) {
            if (normalizedPath.startsWith(DAM_ROOT)) {
                return IndexingScopeDecision.accept();
            }
            return IndexingScopeDecision.reject("resource not found");
        }

        if (isPage(resource)) {
            return IndexingScopeDecision.accept();
        }

        if (isDamAsset(resource)) {
            return evaluateAssetMime(resource, config.getAllowedFiles());
        }

        return IndexingScopeDecision.reject("unsupported node type");
    }

    @Override
    public Resource resolveIndexableResource(final ResourceResolver resolver, final String path) {
        if (resolver == null || path == null || path.isBlank()) {
            return null;
        }

        final String normalizedPath = SearchStaxFullIndexPathConfigurationServiceImpl
                .normalizePath(path, false);
        final Resource resource = resolver.getResource(normalizedPath);
        if (resource == null) {
            return null;
        }

        if (isPage(resource)) {
            final Resource content = resource.getChild("jcr:content");
            return content != null ? content : resource;
        }

        if (isDamAsset(resource)) {
            final Resource metadata = resource.getChild("jcr:content/metadata");
            if (metadata != null) {
                return metadata;
            }
            final Resource content = resource.getChild("jcr:content");
            return content != null ? content : resource;
        }

        return resource;
    }

    private IndexingScopeDecision evaluateAssetMime(final Resource assetResource, final String[] allowedFiles) {
        if (allowedFiles == null || allowedFiles.length == 0) {
            return IndexingScopeDecision.accept();
        }

        final String mimeType = readMimeType(assetResource);
        if (mimeType == null || mimeType.isBlank()) {
            LOG.info("{} Asset {} skipped: MIME type not found", IncrementalIndexingDefaults.LOG_PREFIX,
                    assetResource.getPath());
            return IndexingScopeDecision.reject("asset MIME type not found");
        }

        for (final String allowed : allowedFiles) {
            if (allowed == null || allowed.isBlank()) {
                continue;
            }
            final String normalizedAllowed = allowed.trim().toLowerCase();
            final String normalizedMime = mimeType.trim().toLowerCase();
            if (normalizedMime.equals(normalizedAllowed)
                    || normalizedMime.contains(normalizedAllowed)
                    || normalizedMime.endsWith("/" + normalizedAllowed.replaceFirst("^\\*\\.", ""))) {
                return IndexingScopeDecision.accept();
            }
        }

        return IndexingScopeDecision.reject("asset MIME type not allowed");
    }

    private String readMimeType(final Resource assetResource) {
        final Resource metadata = assetResource.getChild("jcr:content/metadata");
        if (metadata != null) {
            final String mime = metadata.getValueMap().get("dc:format", String.class);
            if (mime != null && !mime.isBlank()) {
                return mime;
            }
        }

        final Resource content = assetResource.getChild("jcr:content");
        if (content != null) {
            return content.getValueMap().get("jcr:mimeType", String.class);
        }

        return null;
    }

    private boolean isUnderAnyRoot(final String path, final String[] rootPaths) {
        for (final String root : rootPaths) {
            if (SearchStaxFullIndexPathConfigurationServiceImpl.isPathUnder(path, root)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPage(final Resource resource) {
        if (hasNodeType(resource, NT_PAGE) || hasNodeType(resource, NT_PAGE_CONTENT)) {
            return true;
        }
        return !resource.getPath().startsWith(DAM_ROOT) && resource.getChild("jcr:content") != null;
    }

    private boolean isDamAsset(final Resource resource) {
        return hasNodeType(resource, NT_ASSET) || hasNodeType(resource, NT_ASSET_CONTENT)
                || resource.getPath().startsWith(DAM_ROOT);
    }

    private boolean hasNodeType(final Resource resource, final String nodeType) {
        try {
            final Node node = resource.adaptTo(Node.class);
            if (node == null) {
                return false;
            }
            return node.isNodeType(nodeType);
        } catch (RepositoryException e) {
            LOG.debug("{} Unable to read node type for {}", IncrementalIndexingDefaults.LOG_PREFIX,
                    resource.getPath(), e);
            return false;
        }
    }
}
