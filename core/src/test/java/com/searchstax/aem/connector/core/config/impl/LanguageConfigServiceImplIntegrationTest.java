package com.searchstax.aem.connector.core.config.impl;

import com.searchstax.aem.connector.core.testcontext.AppAemContext;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import com.searchstax.aem.connector.core.utils.ResolverUtil;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class LanguageConfigServiceImplIntegrationTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ResolverUtil resolverUtil;

    private LanguageConfigServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        when(resolverUtil.getServiceResolver()).thenReturn(context.resourceResolver());
        service = new LanguageConfigServiceImpl();
        TestReflection.inject(service, "resolverUtil", resolverUtil);
    }

    @Test
    void loadsMappingsFromJcr() {
        context.create().resource(
                LanguageConfigServiceImpl.CONFIG_PATH,
                "languageMappings",
                "[{\"aemLanguage\":\"en\",\"searchStaxLanguage\":\"abc\",\"enabled\":true}]");

        assertEquals(1, service.getLanguageMappings().size());
        assertEquals("abc", service.getLanguageMappings().get(0).getSearchStaxLanguage());
    }

    @Test
    void returnsEmptyListForInvalidJson() {
        context.create().resource(
                LanguageConfigServiceImpl.CONFIG_PATH,
                "languageMappings",
                "invalid");

        assertTrue(service.getLanguageMappings().isEmpty());
    }

    @Test
    void returnsEmptyListWhenConfigResourceMissing() {
        assertTrue(service.getLanguageMappings().isEmpty());
    }

    @Test
    void returnsEmptyOptionalWhenConfigJsonIsMalformedForMapping() {
        context.create().resource(
                LanguageConfigServiceImpl.CONFIG_PATH,
                "languageMappings",
                "invalid");

        assertTrue(service.mapToSearchStaxLanguage("en_US").isEmpty());
    }

    @Test
    void returnsEmptyOptionalForBlankLanguage() {
        assertTrue(service.mapToSearchStaxLanguage("  ").isEmpty());
    }

    @Test
    void mapsLanguageFromPersistedConfiguration() {
        context.create().resource(
                LanguageConfigServiceImpl.CONFIG_PATH,
                "languageMappings",
                "[{\"aemLanguage\":\"en_US\",\"searchStaxLanguage\":\"us_en\",\"enabled\":true}]");

        assertEquals(Optional.of("us_en"), service.mapToSearchStaxLanguage("en_US"));
    }

    @Test
    void returnsEmptyListWhenMappingsPropertyBlank() {
        context.create().resource(LanguageConfigServiceImpl.CONFIG_PATH, "languageMappings", " ");

        assertTrue(service.getLanguageMappings().isEmpty());
    }

    @Test
    void skipsMappingsWithBlankAemLanguage() {
        context.create().resource(
                LanguageConfigServiceImpl.CONFIG_PATH,
                "languageMappings",
                "[{\"aemLanguage\":\" \",\"searchStaxLanguage\":\"abc\",\"enabled\":true},"
                        + "{\"aemLanguage\":\"en\",\"searchStaxLanguage\":\"us\",\"enabled\":true}]");

        assertEquals(Optional.of("us"), service.mapToSearchStaxLanguage("en"));
    }

    @Test
    void mapsBaseLanguageWhenExactMatchMissing() {
        context.create().resource(
                LanguageConfigServiceImpl.CONFIG_PATH,
                "languageMappings",
                "[{\"aemLanguage\":\"en\",\"searchStaxLanguage\":\"us\",\"enabled\":true}]");

        assertEquals(Optional.of("us"), service.mapToSearchStaxLanguage("en_US"));
    }
}
