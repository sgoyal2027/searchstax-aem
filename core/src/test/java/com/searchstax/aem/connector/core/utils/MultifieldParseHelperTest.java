package com.searchstax.aem.connector.core.utils;

import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class MultifieldParseHelperTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Test
    void trimsValuesToEmptyString() {
        assertEquals("", MultifieldParseHelper.trimToEmpty(null));
        assertEquals("value", MultifieldParseHelper.trimToEmpty(" value "));
    }

    @Test
    void treatsMissingEnabledFlagAsEnabled() {
        assertTrue(MultifieldParseHelper.isEnabled(new HashMap<>(), "enabled"));
    }

    @Test
    void readsStringArrayParameterFromRequestValues() {
        context.request().setParameterMap(Map.of("./rootPaths", new String[] {" /content/wknd ", ""}));

        assertArrayEquals(
                new String[] {"/content/wknd"},
                MultifieldParseHelper.getStringArrayParameter(context.request(), "rootPaths"));
    }

    @Test
    void fallsBackToLegacyMultifieldParsing() {
        context.request().setParameterMap(Map.of(
                "./allowedFiles/item0/./allowedFiles",
                new String[] {"pdf"}));

        final Map<Integer, Map<String, String>> items =
                MultifieldParseHelper.parseItems(context.request(), "allowedFiles", "allowedFiles");

        assertEquals(1, items.size());
        assertEquals("pdf", items.get(0).get("allowedFiles"));
    }
}
