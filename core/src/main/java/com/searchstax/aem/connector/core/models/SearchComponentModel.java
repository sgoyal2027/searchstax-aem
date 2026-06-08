package com.searchstax.aem.connector.core.models;

import com.searchstax.aem.connector.core.config.search.SearchStaxPublicBindingPaths;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
        adaptables = {SlingHttpServletRequest.class, Resource.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SearchComponentModel {

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String languageOverride;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String renderMethod;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String facetingType;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String searchInputPlaceholder;

    public String getConfigUrl() {
        final StringBuilder url = new StringBuilder(SearchStaxPublicBindingPaths.SERVLET_SEARCH_CONFIG);
        if (languageOverride != null && !languageOverride.isBlank()) {
            url.append("?language=").append(languageOverride.trim());
        }
        return url.toString();
    }

    public String getRenderMethod() {
        return defaultIfBlank(renderMethod, "pagination");
    }

    public String getFacetingType() {
        return defaultIfBlank(facetingType, "and");
    }

    public String getSearchInputPlaceholder() {
        return defaultIfBlank(searchInputPlaceholder, "Search...");
    }

    public String getComponentId() {
        if (request != null && request.getResource() != null) {
            return "searchstax-" + request.getResource().getPath().hashCode();
        }
        return "searchstax-search";
    }

    private static String defaultIfBlank(final String value, final String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
