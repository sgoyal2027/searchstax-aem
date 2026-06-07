package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.incremental.IndexingAction;

public interface IncrementalIndexingQueueService {

    void enqueue(String path, IndexingAction action);
}
