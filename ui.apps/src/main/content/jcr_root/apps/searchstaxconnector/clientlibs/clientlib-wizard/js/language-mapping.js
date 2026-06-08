(function ($, document) {

    if (!window.location.pathname.includes("/languagemapping")) {
        return;
    }

    "use strict";

    var KNOWN_AEM_LANGUAGES = [
        "en", "en_US", "en_GB", "de", "de_DE", "fr", "fr_FR", "es", "es_ES",
        "it", "it_IT", "pt", "pt_BR", "nl", "nl_NL", "ja", "ja_JP", "zh",
        "zh_CN", "zh_TW", "ko", "ko_KR", "ar", "ru"
    ];

    $(document).ready(function () {

        Coral.commons.ready(document, function () {

            $.getJSON(
                "/bin/searchstaxconnector/wizard/language-mappings-load",
                function (data) {

                    if (data && data.length > 0) {
                        populateLanguageMappings(data);
                    } else {
                        initializeAllMultifieldItems();
                    }
                }
            );
        });

        SearchStaxConfigUtil.attachSaveHandlers(
            "/bin/searchstaxconnector/wizard/language-mappings",
            "Language mappings saved successfully."
        );
    });


    function resolveAemLanguageType(aemLanguage) {

        if (!aemLanguage) {
            return "en";
        }

        for (var i = 0; i < KNOWN_AEM_LANGUAGES.length; i++) {
            if (KNOWN_AEM_LANGUAGES[i] === aemLanguage) {
                return aemLanguage;
            }
        }

        return "custom";
    }

    function initializeMultifieldItem(item) {

        SearchStaxConfigUtil.setEnabledSelect(item, false);

        var aemLanguageType = item.querySelector("coral-select[name*='aemLanguageType']");
        var customAemLanguage = item.querySelector("input[name*='customAemLanguage']");
        var searchStaxLanguage = item.querySelector("input[name*='searchStaxLanguage']");

        if (!aemLanguageType) {
            return;
        }

        Coral.commons.ready(aemLanguageType, function () {

            var value = aemLanguageType.value;

            if (!value && aemLanguageType.items && aemLanguageType.items.length > 0) {
                value = aemLanguageType.items.getAll()[0].value;
                aemLanguageType.value = value;
            }

            toggleCustomLanguageField(item, value);

            if (value !== "custom" && searchStaxLanguage && !searchStaxLanguage.value) {
                searchStaxLanguage.value = value.split("_")[0];
                searchStaxLanguage.dispatchEvent(new Event("input", { bubbles: true }));
                searchStaxLanguage.dispatchEvent(new Event("change", { bubbles: true }));
            }

            if (value === "custom" && customAemLanguage) {
                customAemLanguage.value = "";
            }
        });
    }

    function initializeAllMultifieldItems() {

        $("coral-multifield-item").each(function () {
            initializeMultifieldItem(this);
        });
    }

    function populateLanguageMappings(mappings) {

        var multifield = $("coral-multifield")[0];

        if (!multifield) {
            return;
        }

        SearchStaxConfigUtil.clearMultifield(multifield);

        mappings.forEach(function (mapping, index) {

            multifield.items.add();

            setTimeout(function () {

                var items = multifield.items.getAll();
                var item = items[index];

                if (!item) {
                    return;
                }

                var aemLanguageType = item.querySelector("coral-select[name*='aemLanguageType']");
                var customAemLanguage = item.querySelector("input[name*='customAemLanguage']");
                var searchStaxLanguage = item.querySelector("input[name*='searchStaxLanguage']");
                var resolvedType = resolveAemLanguageType(mapping.aemLanguage);

                if (searchStaxLanguage) {
                    searchStaxLanguage.value = mapping.searchStaxLanguage || "";
                    searchStaxLanguage.dispatchEvent(new Event("input", { bubbles: true }));
                }

                if (resolvedType === "custom" && customAemLanguage) {
                    customAemLanguage.value = mapping.aemLanguage || "";
                    customAemLanguage.dispatchEvent(new Event("change", { bubbles: true }));
                }

                if (aemLanguageType) {
                    Coral.commons.ready(aemLanguageType, function () {
                        aemLanguageType.value = resolvedType;
                        aemLanguageType.__initTriggered = true;
                        aemLanguageType.dispatchEvent(new Event("change", { bubbles: true }));
                    });
                }

                SearchStaxConfigUtil.setEnabledSelect(item, mapping.enabled);

            }, 50);
        });
    }

    function toggleCustomLanguageField(item, value) {

        var customAemLanguage = item.querySelector("input[name*='customAemLanguage']");
        var container = customAemLanguage ? customAemLanguage.closest("div") : null;

        if (!container) {
            return;
        }

        if (value === "custom") {
            container.style.display = "block";
        } else {
            container.style.display = "none";
        }
    }

    function handleAemLanguageChange(item) {

        var aemLanguageType = item.querySelector("coral-select[name*='aemLanguageType']");
        var customAemLanguage = item.querySelector("input[name*='customAemLanguage']");
        var searchStaxLanguage = item.querySelector("input[name*='searchStaxLanguage']");

        if (!aemLanguageType) {
            return;
        }

        var value = aemLanguageType.value;
        toggleCustomLanguageField(item, value);

        if (value === "custom") {
            if (customAemLanguage) {
                customAemLanguage.value = "";
            }
            if (searchStaxLanguage) {
                searchStaxLanguage.value = "";
            }
            return;
        }

        if (searchStaxLanguage) {
            searchStaxLanguage.value = value.split("_")[0];
            searchStaxLanguage.dispatchEvent(new Event("input", { bubbles: true }));
            searchStaxLanguage.dispatchEvent(new Event("change", { bubbles: true }));
        }
    }

    $(document).on("change", "coral-select[name*='aemLanguageType']", function () {

        var item = $(this).closest("coral-multifield-item")[0];

        if (!item) {
            return;
        }

        if (this.__initTriggered) {
            this.__initTriggered = false;
            toggleCustomLanguageField(item, this.value);
            return;
        }

        handleAemLanguageChange(item);
    });

    $(document).on("click", "[coral-multifield-add]", function () {

        setTimeout(function () {

            var items = $("coral-multifield-item");

            if (items.length > 0) {
                initializeMultifieldItem(items[items.length - 1]);
            }

        }, 500);
    });

})(Granite.$, document);
