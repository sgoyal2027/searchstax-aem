package com.searchstax.aem.connector.core.services;

import com.searchstax.aem.connector.core.constants.SearchStaxFullIndexDefaults;
import com.searchstax.aem.connector.core.services.impl.SearchStaxFullIndexRunServiceImpl;
import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.event.jobs.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class FullIndexPathConfigTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private Job job;

    @Test
    void fromDefaultsReturnsEmptyConfiguration() {
        final FullIndexPathConfig config = FullIndexPathConfig.fromDefaults();

        assertTrue(config.isEmpty());
        assertEquals("", config.getRootPath());
    }

    @Test
    void fromRequestParsesRootIncludeAndExcludePaths() {
        context.request().addRequestParameter("rootPath", "/content/wknd");
        context.request().addRequestParameter("includePaths", "/content/wknd/us/en");
        context.request().addRequestParameter("includeChildPaths", "true");
        context.request().addRequestParameter("excludePaths", "/content/wknd/private");

        final FullIndexPathConfig config = FullIndexPathConfig.fromRequest(context.request());

        assertEquals("/content/wknd", config.getRootPath());
        assertEquals(1, config.getIncludePaths().length);
        assertEquals("/content/wknd/us/en", config.getIncludePaths()[0]);
        assertTrue(config.getIncludeChildPaths()[0]);
        assertEquals("/content/wknd/private", config.getExcludePaths()[0]);
    }

    @Test
    void fromJobPropertiesRoundTripsThroughToJobProperties() {
        final Map<String, Object> props = new HashMap<>();
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH, "/content/wknd");
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS, new String[] {"/content/wknd/us/en"});
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_CHILD_PATHS, new String[] {"true"});
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_EXCLUDE_PATHS, new String[] {"/content/wknd/x"});

        final FullIndexPathConfig config = FullIndexPathConfig.fromJobProperties(props);
        final Map<String, Object> roundTrip = config.toJobProperties();

        assertEquals("/content/wknd", roundTrip.get(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH));
        assertEquals("/content/wknd/us/en", ((String[]) roundTrip.get(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS))[0]);
    }

    @Test
    void fromJobReadsConfiguredProperties() {
        final Set<String> propertyNames = new HashSet<>();
        propertyNames.add(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH);
        propertyNames.add(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS);
        when(job.getPropertyNames()).thenReturn(propertyNames);
        when(job.getProperty(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH)).thenReturn("/content/wknd");
        when(job.getProperty(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS))
                .thenReturn(new String[] {"/content/wknd/us/en"});

        final FullIndexPathConfig config = FullIndexPathConfig.fromJob(job);

        assertEquals("/content/wknd", config.getRootPath());
        assertEquals(1, config.getIncludePaths().length);
    }

    @Test
    void fromRequestReturnsDefaultsWhenAllParamsEmpty() {
        final FullIndexPathConfig config = FullIndexPathConfig.fromRequest(context.request());

        assertTrue(config.isEmpty());
        assertEquals("", config.getRootPath());
        assertEquals(0, config.getIncludePaths().length);
        assertEquals(0, config.getExcludePaths().length);
    }

    @Test
    void fromJobReturnsDefaultsForNullJob() {
        final FullIndexPathConfig config = FullIndexPathConfig.fromJob(null);

        assertTrue(config.isEmpty());
    }

    @Test
    void fromJobPropertiesReturnsDefaultsForNullOrEmptyMap() {
        assertTrue(FullIndexPathConfig.fromJobProperties(null).isEmpty());
        assertTrue(FullIndexPathConfig.fromJobProperties(new HashMap<>()).isEmpty());
    }

    @Test
    void fromJobPropertiesReturnsDefaultsWhenRootAndIncludesEmpty() {
        final Map<String, Object> props = new HashMap<>();
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH, " ");
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS, new String[0]);
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_EXCLUDE_PATHS, new String[] {"/content/ignored"});

        final FullIndexPathConfig config = FullIndexPathConfig.fromJobProperties(props);

        assertTrue(config.isEmpty());
        assertEquals(0, config.getExcludePaths().length);
    }

    @Test
    void isEmptyReturnsFalseWhenRootPathSet() {
        final Map<String, Object> props = new HashMap<>();
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH, "/content/wknd");
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS, new String[0]);

        final FullIndexPathConfig config = FullIndexPathConfig.fromJobProperties(props);

        assertFalse(config.isEmpty());
        assertEquals("/content/wknd", config.getRootPath());
    }

    @Test
    void fromRequestParsesMultifieldItemParamsSkippingTypeHints() {
        context.request().addRequestParameter(
                "includePaths/item0/./includePaths",
                "/content/wknd/us/en");
        context.request().addRequestParameter("includePaths/item0/@TypeHint", "java.lang.String");
        context.request().addRequestParameter(
                "includePaths/item1/./includePaths",
                "/content/wknd/us/fr");

        final FullIndexPathConfig config = FullIndexPathConfig.fromRequest(context.request());

        assertEquals(2, config.getIncludePaths().length);
        assertTrue(Arrays.asList(config.getIncludePaths()).contains("/content/wknd/us/en"));
        assertTrue(Arrays.asList(config.getIncludePaths()).contains("/content/wknd/us/fr"));
        assertFalse(Arrays.asList(config.getIncludePaths()).contains("java.lang.String"));
    }

    @Test
    void stringArrayPropertyFromJobPropertiesAcceptsStringScalar() {
        final Map<String, Object> props = new HashMap<>();
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH, "/content/wknd");
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS, "/content/wknd/us/en");

        final FullIndexPathConfig config = FullIndexPathConfig.fromJobProperties(props);

        assertEquals(1, config.getIncludePaths().length);
        assertEquals("/content/wknd/us/en", config.getIncludePaths()[0]);
    }

    @Test
    void stringArrayPropertyFromJobPropertiesAcceptsObjectArrayWithNulls() {
        final Map<String, Object> props = new HashMap<>();
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH, "/content/wknd");
        props.put(
                SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS,
                new Object[] {"/content/a", null, " ", "/content/b"});

        final FullIndexPathConfig config = FullIndexPathConfig.fromJobProperties(props);

        assertEquals(2, config.getIncludePaths().length);
        assertTrue(Arrays.asList(config.getIncludePaths()).contains("/content/a"));
        assertTrue(Arrays.asList(config.getIncludePaths()).contains("/content/b"));
    }

    @Test
    void fromJobPropertiesIgnoresUnsupportedPropertyTypes() {
        final Map<String, Object> props = new HashMap<>();
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH, "/content/wknd");
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS, 42);

        final FullIndexPathConfig config = FullIndexPathConfig.fromJobProperties(props);

        assertEquals(0, config.getIncludePaths().length);
    }

    @Test
    void booleanArrayFromStringsHandlesNullValuesArray() {
        final Map<String, Object> props = new HashMap<>();
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH, "/content/wknd");
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS, new String[] {"/content/a"});
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_CHILD_PATHS, (String[]) null);

        final FullIndexPathConfig config = FullIndexPathConfig.fromJobProperties(props);

        assertTrue(config.getIncludeChildPaths()[0]);
    }

    @Test
    void booleanArrayFromStringsHandlesNullElements() {
        final Map<String, Object> props = new HashMap<>();
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_ROOT_PATH, "/content/wknd");
        props.put(SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_PATHS, new String[] {"/content/a", "/content/b"});
        props.put(
                SearchStaxFullIndexDefaults.JOB_PROP_INCLUDE_CHILD_PATHS,
                new String[] {"true", null});

        final FullIndexPathConfig config = FullIndexPathConfig.fromJobProperties(props);

        assertTrue(config.getIncludeChildPaths()[0]);
        assertFalse(config.getIncludeChildPaths()[1]);
    }
}
