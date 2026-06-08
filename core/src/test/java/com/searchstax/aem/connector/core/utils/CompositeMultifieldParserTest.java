package com.searchstax.aem.connector.core.utils;

import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class CompositeMultifieldParserTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Test
    void parsesGraniteCompositeMultifieldParameters() {
        context.request().setParameterMap(Map.of(
                "languageMappings/item0/./aemLanguageType",
                new String[] {"en"},
                "languageMappings/item0/./searchStaxLanguage",
                new String[] {"en"}));

        final Map<Integer, Map<String, String>> items =
                CompositeMultifieldParser.parse(context.request(), "languageMappings");

        assertEquals(1, items.size());
        assertEquals("en", items.get(0).get("aemLanguageType"));
        assertEquals("en", items.get(0).get("searchStaxLanguage"));
    }

    @Test
    void detectsCheckedMultifieldValues() {
        final Map<String, String> item = new HashMap<>();
        item.put("enabled", "true");

        assertTrue(CompositeMultifieldParser.isChecked(item, "enabled"));
        assertFalse(CompositeMultifieldParser.isChecked(item, "missing"));
    }

    @Test
    void treatsOnValueAsChecked() {
        final Map<String, String> item = new HashMap<>();
        item.put("enabled", "on");

        assertTrue(CompositeMultifieldParser.isChecked(item, "enabled"));
    }

    @Test
    void getValueReturnsNullForMissingInputs() {
        assertEquals(null, CompositeMultifieldParser.getValue(null, "enabled"));
        assertEquals(null, CompositeMultifieldParser.getValue(new HashMap<>(), null));
    }

    @Test
    void skipsParametersWithNullValues() {
        final Map<String, Object> params = new HashMap<>();
        params.put("languageMappings/item0/./aemLanguageType", new String[] {null});
        context.request().setParameterMap(params);

        assertTrue(CompositeMultifieldParser.parse(context.request(), "languageMappings").isEmpty());
    }

    @Test
    void prefersCheckedValueWhenCheckboxSubmitsMultipleValues() {
        context.request().setParameterMap(Map.of(
                "metadataMappings/item0/./enabled",
                new String[] {"false", "true"}));

        final Map<Integer, Map<String, String>> items =
                CompositeMultifieldParser.parse(context.request(), "metadataMappings");

        assertEquals(1, items.size());
        assertEquals("true", items.get(0).get("enabled"));
        assertTrue(CompositeMultifieldParser.isChecked(items.get(0), "enabled"));
    }

    @Test
    void parsesUncheckedCheckboxValue() {
        context.request().setParameterMap(Map.of(
                "metadataMappings/item0/./enabled",
                new String[] {"false"}));

        final Map<Integer, Map<String, String>> items =
                CompositeMultifieldParser.parse(context.request(), "metadataMappings");

        assertEquals(1, items.size());
        assertEquals("false", items.get(0).get("enabled"));
        assertFalse(CompositeMultifieldParser.isChecked(items.get(0), "enabled"));
    }

    @Test
    void parsesParametersWithAtSuffixInKey() {
        context.request().setParameterMap(Map.of(
                "includePaths/item0/./path@Delete",
                new String[] {"/content/wknd"}));

        final Map<Integer, Map<String, String>> items =
                CompositeMultifieldParser.parse(context.request(), "includePaths");

        assertEquals(1, items.size());
        assertEquals("/content/wknd", items.get(0).get("path"));
    }
}
