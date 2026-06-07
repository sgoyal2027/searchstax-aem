package com.searchstax.aem.connector.core.constants;

public final class IncrementalIndexingDefaults {

    public static final String JOB_TOPIC = "searchstax/incremental";

    public static final String JOB_PROP_INDEX_PATHS = "indexPaths";
    public static final String JOB_PROP_DELETE_PATHS = "deletePaths";
    public static final String JOB_PROP_BATCH_ID = "batchId";

    public static final long DEBOUNCE_MS = 10_000L;

    public static final String AUDIT_ROOT = "/var/searchstaxconnector/incremental/audit";

    public static final long AUDIT_RETENTION_MS = 24L * 60L * 60L * 1000L;

    public static final String LOG_PREFIX = "[SearchStax-Incremental]";

    public static final String DOCUMENT_ID_FIELD = "id";

    public static final String LANGUAGE_FIELD = "language_s";

    private IncrementalIndexingDefaults() {
    }
}
