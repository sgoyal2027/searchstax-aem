package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.dto.response.IndexingBatchResult;

import java.util.List;

public interface IndexingApiService {

    IndexingBatchResult indexDocuments(List<String> documentJsonBodies);

    IndexingBatchResult deleteDocuments(List<String> documentIds);
}
