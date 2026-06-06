package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.dto.request.EmailRequest;

public interface EmailService {

    void sendEmail(EmailRequest request);
}
