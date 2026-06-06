package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.dto.response.ApiResponse;

public interface SearchstaxClientService {

    ApiResponse indexDocument(String requestJson);

    ApiResponse deleteDocument(String deleteJson);

}
