package com.searchstax.aem.connector.core.config;

import com.searchstax.aem.connector.core.config.model.LanguageMappingConfig;

import java.util.List;
import java.util.Optional;

public interface LanguageConfigService {

    List<LanguageMappingConfig> getLanguageMappings();

    Optional<String> mapToSearchStaxLanguage(String aemLanguage);
}
