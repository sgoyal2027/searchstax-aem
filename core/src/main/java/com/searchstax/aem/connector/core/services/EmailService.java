package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.dto.request.EmailRequest;

public interface EmailService {

    boolean sendEmail(EmailRequest request);

    String getLastSendError();
}
