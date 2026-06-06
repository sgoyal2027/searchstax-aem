package com.searchstax.aem.connector.core.services.impl;

import com.searchstax.aem.connector.core.services.FullIndexPathConfig;
import com.searchstax.aem.connector.core.services.FullIndexProgress;
import com.searchstax.aem.connector.core.services.FullIndexProgress.State;
import com.searchstax.aem.connector.core.services.FullIndexTriggerResult;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexRunService;
import org.osgi.service.component.annotations.Component;

/**
 * Stub until Step 3 indexing logic is implemented.
 */
@Component(service = SearchStaxFullIndexRunService.class)
public class SearchStaxFullIndexRunServiceImpl implements SearchStaxFullIndexRunService {

    private static final String NOT_AVAILABLE_MESSAGE =
            "Full index execution is not available yet. Indexing logic will be added in a future release.";

    @Override
    public FullIndexTriggerResult triggerFullIndex(final FullIndexPathConfig config) {
        return new FullIndexTriggerResult(false, "", NOT_AVAILABLE_MESSAGE, 503);
    }

    @Override
    public FullIndexProgress getProgress() {
        return new FullIndexProgress(
                State.IDLE,
                0L,
                0L,
                0L,
                0L,
                0L,
                0,
                "",
                0L,
                0L,
                NOT_AVAILABLE_MESSAGE);
    }
}
