package com.searchstax.aem.connector.core.constants;

/**
 * Hardcoded full-index settings for Phase 1. Future: move to OSGi / UI without renaming fields.
 *
 * <p>Traversal mode: run QB predicate validation on target AEM (see
 * {@code core/src/main/resources/searchstax-fullindex-traversal-validation.md}) before switching
 * from {@link TraversalMode#JCR_SQL2} to {@link TraversalMode#QUERY_BUILDER}.
 */
public final class SearchStaxFullIndexDefaults {

    public static final String JOB_TOPIC = "searchstax/fullindex";

    public static final boolean DEFAULT_INCLUDE_CHILD_PAGES = true;

    public static final String JOB_PROP_ROOT_PATH = "rootPath";
    public static final String JOB_PROP_INCLUDE_PATHS = "includePaths";
    public static final String JOB_PROP_EXCLUDE_PATHS = "excludePaths";
    public static final String JOB_PROP_CHILD_PAGES = "childPages";
    public static final String JOB_PROP_INCLUDE_CHILD_PATHS = "includeChildPaths";

    public static final String DAM_ROOT = "/content/dam";

    public static final int BATCH_SIZE = 100;

    public static final int QUERY_PAGE_SIZE = 500;

    /**
     * Default until QB keyset validation passes on target AEM; then set to {@link TraversalMode#QUERY_BUILDER}.
     */
    public static final TraversalMode TRAVERSAL_MODE = TraversalMode.JCR_SQL2;

    public static final long COMMIT_WITHIN_MS = 120_000L;

    public static final int HARD_COMMIT_EVERY_N_BATCHES = 10;

    public static final boolean HARD_COMMIT_ON_SUCCESS = true;

    public static final long BATCH_THROTTLE_MS = 300L;

    public static final int RESOLVER_REFRESH_EVERY_N_BATCHES = 20;

    public static final String PAGE_TITLE_FIELD_FORMAT = "title_txt_%s";
    public static final String PAGE_DESCRIPTION_FIELD_FORMAT = "description_txt_%s";
    public static final String PAGE_TAGS_FIELD_FORMAT = "tags_ss_%s";
    public static final String PAGE_CONTENT_FIELD_FORMAT = "content_txts_%s";
    public static final String ASSET_DEFAULT_LANGUAGE = "en";

    private SearchStaxFullIndexDefaults() {
    }

    public enum TraversalMode {
        QUERY_BUILDER,
        JCR_SQL2
    }
}
