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
}
