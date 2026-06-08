package com.searchstax.aem.connector.core.servlets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.services.FullIndexProgress;
import com.searchstax.aem.connector.core.services.SearchStaxFullIndexRunService;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchStaxFullIndexStatusServletTest {

    private final AemContext context = AppAemContext.newAemContext();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SearchStaxFullIndexRunService fullIndexRunService;

    @Mock
    private JobManager jobManager;

    @Mock
    private Job job;

    @BeforeEach
    void setUp() {
        context.registerService(SearchStaxFullIndexRunService.class, fullIndexRunService);
        context.registerService(JobManager.class, jobManager);
    }

    @Test
    void returnsIdleProgressWhenNoJobs() throws Exception {
        when(fullIndexRunService.getProgress()).thenReturn(new FullIndexProgress(
                FullIndexProgress.State.IDLE,
                0L, 0L, 0L, 0L, 0L, 0, "", 0L, 0L, "Not available"));
        when(jobManager.findJobs(any(), eq(SearchStaxFullIndexDefaults.JOB_TOPIC), eq(-1L), isNull()))
                .thenReturn(Collections.emptyList());

        final SearchStaxFullIndexStatusServlet servlet =
                context.registerInjectActivateService(new SearchStaxFullIndexStatusServlet());
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertEquals("IDLE", json.get("state").asText());
        assertFalse(json.get("running").asBoolean());
    }

    @Test
    void overridesStateToRunningWhenActiveJobExists() throws Exception {
        when(fullIndexRunService.getProgress()).thenReturn(new FullIndexProgress(
                FullIndexProgress.State.IDLE,
                0L, 0L, 0L, 0L, 0L, 0, "", 0L, 0L, "Not available"));
        when(job.getId()).thenReturn("job-123");
        when(jobManager.findJobs(
                eq(JobManager.QueryType.ACTIVE),
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                eq(-1L),
                isNull()))
                .thenReturn(Collections.singletonList(job));
        when(jobManager.findJobs(
                eq(JobManager.QueryType.QUEUED),
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                eq(-1L),
                isNull()))
                .thenReturn(Collections.emptyList());

        final SearchStaxFullIndexStatusServlet servlet = new SearchStaxFullIndexStatusServlet();
        TestReflection.inject(servlet, "searchStaxFullIndexRunService", fullIndexRunService);
        TestReflection.inject(servlet, "jobManager", jobManager);
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertEquals("RUNNING", json.get("state").asText());
        assertEquals("job-123", json.get("jobId").asText());
        assertTrue(json.get("running").asBoolean());
    }

    @Test
    void overridesStateToRunningWhenOnlyQueuedJobExists() throws Exception {
        when(fullIndexRunService.getProgress()).thenReturn(new FullIndexProgress(
                FullIndexProgress.State.IDLE,
                0L, 0L, 0L, 0L, 0L, 0, "", 0L, 0L, "Not available"));
        when(job.getId()).thenReturn("job-queued-123");

        when(jobManager.findJobs(
                eq(JobManager.QueryType.ACTIVE),
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                eq(-1L),
                isNull()))
                .thenReturn(Collections.emptyList());
        when(jobManager.findJobs(
                eq(JobManager.QueryType.QUEUED),
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                eq(-1L),
                isNull()))
                .thenReturn(Collections.singletonList(job));

        final SearchStaxFullIndexStatusServlet servlet = new SearchStaxFullIndexStatusServlet();
        TestReflection.inject(servlet, "searchStaxFullIndexRunService", fullIndexRunService);
        TestReflection.inject(servlet, "jobManager", jobManager);
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertEquals("RUNNING", json.get("state").asText());
        assertEquals("job-queued-123", json.get("jobId").asText());
        assertTrue(json.get("running").asBoolean());
    }

    @Test
    void reportsCompleteWhenFinishedWithoutActiveJob() throws Exception {
        when(fullIndexRunService.getProgress()).thenReturn(new FullIndexProgress(
                FullIndexProgress.State.SUCCESS,
                10L, 10L, 0L, 2L, 1L, 1, "/content/wknd/us/en/page", 1_000L, 50L, "done"));

        when(jobManager.findJobs(any(), eq(SearchStaxFullIndexDefaults.JOB_TOPIC), eq(-1L), isNull()))
                .thenReturn(Collections.emptyList());

        final SearchStaxFullIndexStatusServlet servlet = new SearchStaxFullIndexStatusServlet();
        TestReflection.inject(servlet, "searchStaxFullIndexRunService", fullIndexRunService);
        TestReflection.inject(servlet, "jobManager", jobManager);
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertEquals("SUCCESS", json.get("state").asText());
        assertFalse(json.get("running").asBoolean());
        assertTrue(json.get("complete").asBoolean());
    }

    @Test
    void firstJobIdReturnsEmptyForNullJob() throws Exception {
        when(fullIndexRunService.getProgress()).thenReturn(new FullIndexProgress(
                FullIndexProgress.State.IDLE,
                0L, 0L, 0L, 0L, 0L, 0, "", 0L, 0L, "Not available"));

        when(jobManager.findJobs(
                eq(JobManager.QueryType.ACTIVE),
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                eq(-1L),
                isNull()))
                .thenReturn(Collections.singletonList(null));
        when(jobManager.findJobs(
                eq(JobManager.QueryType.QUEUED),
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                eq(-1L),
                isNull()))
                .thenReturn(Collections.emptyList());

        final SearchStaxFullIndexStatusServlet servlet = new SearchStaxFullIndexStatusServlet();
        TestReflection.inject(servlet, "searchStaxFullIndexRunService", fullIndexRunService);
        TestReflection.inject(servlet, "jobManager", jobManager);
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertEquals("", json.get("jobId").asText());
    }

    @Test
    void firstJobIdReturnsEmptyForNullJobId() throws Exception {
        when(fullIndexRunService.getProgress()).thenReturn(new FullIndexProgress(
                FullIndexProgress.State.IDLE,
                0L, 0L, 0L, 0L, 0L, 0, "", 0L, 0L, "Not available"));
        when(job.getId()).thenReturn(null);

        when(jobManager.findJobs(
                eq(JobManager.QueryType.ACTIVE),
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                eq(-1L),
                isNull()))
                .thenReturn(Collections.singletonList(job));
        when(jobManager.findJobs(
                eq(JobManager.QueryType.QUEUED),
                eq(SearchStaxFullIndexDefaults.JOB_TOPIC),
                eq(-1L),
                isNull()))
                .thenReturn(Collections.emptyList());

        final SearchStaxFullIndexStatusServlet servlet = new SearchStaxFullIndexStatusServlet();
        TestReflection.inject(servlet, "searchStaxFullIndexRunService", fullIndexRunService);
        TestReflection.inject(servlet, "jobManager", jobManager);
        servlet.doGet(context.request(), context.response());

        final JsonNode json = objectMapper.readTree(context.response().getOutputAsString());
        assertEquals("", json.get("jobId").asText());
    }

    @Test
    void abbreviateMiddleShortensLongPaths() {
        final String path = "/content/wknd/us/en/magazine/very/long/path/that/exceeds/the/maximum/allowed/length/for/display";
        final String abbreviated = SearchStaxFullIndexStatusServlet.abbreviateMiddle(path, 40);

        assertTrue(abbreviated.length() <= 40);
        assertTrue(abbreviated.contains("..."));
    }

    @Test
    void abbreviateMiddleReturnsEmptyForNull() {
        assertEquals("", SearchStaxFullIndexStatusServlet.abbreviateMiddle(null, 40));
    }

    @Test
    void abbreviateMiddleTruncatesWithoutEllipsisWhenMaxLengthThree() {
        assertEquals("abc", SearchStaxFullIndexStatusServlet.abbreviateMiddle("abcdef", 3));
    }

    @Test
    void abbreviateMiddleReturnsUnchangedForShortValues() {
        assertEquals(
                "/content/a",
                SearchStaxFullIndexStatusServlet.abbreviateMiddle("/content/a", 40));
    }
}
