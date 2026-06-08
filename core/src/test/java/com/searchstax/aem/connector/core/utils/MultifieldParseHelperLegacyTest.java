package com.searchstax.aem.connector.core.utils;

import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(AemContextExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MultifieldParseHelperLegacyTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Test
    void parseLegacyItemsUsesAlternatePrefixesAndMarkerFields() {
        try (MockedStatic<CompositeMultifieldParser> parser = mockStatic(CompositeMultifieldParser.class)) {
            parser.when(() -> CompositeMultifieldParser.parse(context.request(), "languageMappings"))
                    .thenReturn(Collections.emptyMap());

            context.request().setParameterMap(Map.of(
                    "languageMappings/item0/customAemLanguage", "en_GB",
                    "languageMappings/item0/searchStaxLanguage", "gb",
                    "languageMappings/item0/enabled", "true"));

            final Map<Integer, Map<String, String>> items = MultifieldParseHelper.parseItems(
                    context.request(),
                    "languageMappings",
                    "aemLanguageType",
                    "customAemLanguage",
                    "searchStaxLanguage",
                    "enabled");

            assertEquals(1, items.size());
            assertEquals("en_GB", items.get(0).get("aemLanguageType"));
            assertEquals("gb", items.get(0).get("searchStaxLanguage"));
            assertEquals("true", items.get(0).get("enabled"));
        }
    }

    @Test
    void parseLegacyItemsUsesSecondMarkerFieldWhenFirstMissing() {
        try (MockedStatic<CompositeMultifieldParser> parser = mockStatic(CompositeMultifieldParser.class)) {
            parser.when(() -> CompositeMultifieldParser.parse(context.request(), "metadataMappings"))
                    .thenReturn(Collections.emptyMap());

            context.request().setParameterMap(Map.of(
                    "./metadataMappings/item0/./customProperty", "customField",
                    "./metadataMappings/item0/./indexFieldName", "title"));

            final Map<Integer, Map<String, String>> items = MultifieldParseHelper.parseItems(
                    context.request(),
                    "metadataMappings",
                    "mappingType",
                    "customProperty",
                    "indexFieldName");

            assertEquals(1, items.size());
            assertEquals("customField", items.get(0).get("mappingType"));
            assertEquals("title", items.get(0).get("indexFieldName"));
        }
    }

    @Test
    void getStringArrayParameterUsesLegacyMultifieldItems() {
        try (MockedStatic<CompositeMultifieldParser> parser = mockStatic(CompositeMultifieldParser.class)) {
            parser.when(() -> CompositeMultifieldParser.parse(context.request(), "rootPaths"))
                    .thenReturn(Collections.emptyMap());

            context.request().setParameterMap(Map.of(
                    "./rootPaths/item0/./rootPaths", "/content/wknd",
                    "./rootPaths/item1/./rootPaths", "/content/other"));

            assertArrayEquals(
                    new String[] {"/content/wknd", "/content/other"},
                    MultifieldParseHelper.getStringArrayParameter(context.request(), "rootPaths"));
        }
    }

    @Test
    void removeBlankValuesFiltersNullAndWhitespaceEntries() {
        assertArrayEquals(
                new String[] {"value"},
                invokeRemoveBlankValues(new String[] {null, " ", "value", ""}));
    }

    private static String[] invokeRemoveBlankValues(final String[] values) {
        try {
            final var method = MultifieldParseHelper.class.getDeclaredMethod("removeBlankValues", String[].class);
            method.setAccessible(true);
            return (String[]) method.invoke(null, (Object) values);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
