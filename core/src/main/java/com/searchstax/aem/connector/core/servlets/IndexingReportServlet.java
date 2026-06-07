package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.searchstax.aem.connector.core.incremental.IndexingAuditRecord;
import com.searchstax.aem.connector.core.services.IndexingAuditService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.List;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.paths=/bin/searchstaxconnector/wizard/indexing-report",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET
        }
)
public class IndexingReportServlet extends SlingSafeMethodsServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Reference
    private transient IndexingAuditService indexingAuditService;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {

        final List<IndexingAuditRecord> records = indexingAuditService.getRecordsForLast24Hours();
        final ArrayNode arrayNode = objectMapper.createArrayNode();

        for (final IndexingAuditRecord record : records) {
            final ObjectNode item = objectMapper.createObjectNode();
            item.put("timestamp", record.getTimestamp());
            item.put("path", record.getPath());
            item.put("action", record.getAction() == null ? "" : record.getAction().name());
            item.put("status", record.getStatus());
            item.put("batchId", record.getBatchId());
            item.put("httpStatus", record.getHttpStatus());
            item.put("message", record.getMessage());
            item.put("durationMs", record.getDurationMs());
            item.put("documentId", record.getDocumentId());
            arrayNode.add(item);
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), arrayNode);
    }
}
