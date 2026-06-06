package com.searchstax.aem.connector.core.config.wizard;

/**
 * Servlet paths used by SearchStax connector wizards.
 */
public final class SearchStaxWizardBindingPaths {

    private static final String SERVLET_BASE = "/bin/searchstaxconnector/wizard";

    public static final String SERVLET_FULL_INDEX_RUN = SERVLET_BASE + "/fullindex-run";

    public static final String SERVLET_FULL_INDEX_STATUS = SERVLET_BASE + "/fullindex-status";

    private SearchStaxWizardBindingPaths() {
    }
}
