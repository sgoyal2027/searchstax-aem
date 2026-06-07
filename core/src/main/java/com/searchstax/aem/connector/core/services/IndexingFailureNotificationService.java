package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.dto.response.IndexingBatchResult;
import com.searchstax.aem.connector.core.incremental.IndexingAction;

import java.util.List;

public interface IndexingFailureNotificationService {

    void notifyBatchFailure(
            String batchId,
            IndexingAction action,
            IndexingBatchResult result,
            List<String> paths);
}
