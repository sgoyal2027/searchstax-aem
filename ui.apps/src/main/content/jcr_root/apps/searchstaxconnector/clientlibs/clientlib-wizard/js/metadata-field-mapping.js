(function ($, document) {
     if (!window.location.pathname.includes("/metadatafieldmapping")) {
        console.log("[MF DEBUG] Not metadata mapping page → skipping script");
        return;
    }
    "use strict";

    var __MF_INITIAL_LOAD = true;

    $(document).ready(function () {

        Coral.commons.ready(document, function () {

            $.getJSON(
                "/bin/searchstaxconnector/wizard/metadata-field-mappings-load",
                function (data) {

                    if (data && data.length > 0) {
                        populateMetadataMappings(data);
                    } else {
                        initializeAllMultifieldItems();
                    }

                    setTimeout(function () {
                        __MF_INITIAL_LOAD = false;
                    }, 500);

                }
            );
        });

        SearchStaxConfigUtil.attachSaveHandlers(
            "/bin/searchstaxconnector/wizard/metadata-field-mappings",
            "Metadata mappings saved successfully."
        );
    });


    // ======================================================
    // HELPERS
    // ======================================================
    function formatIndexFieldName(value) {

        if (!value || value === "custom") {
            return "";
        }

        if (value.indexOf(":") !== -1) {
            return value.split(":")[1];
        }

        return value;
    }

    var MULTI_VALUE_AEM_FIELDS = ["cq:tags", "dc:subject"];

    function setTextFieldValue(item, nameFragment, value) {
        if (!item) {
            return;
        }

        var textValue = value || "";
        var coralField = item.querySelector("coral-textfield[name*='" + nameFragment + "']");
        var input = item.querySelector("input[name*='" + nameFragment + "']");

        if (coralField) {
            Coral.commons.ready(coralField, function () {
                coralField.value = textValue;
            });
        }

        if (input) {
            input.value = textValue;
            input.dispatchEvent(new Event("input", { bubbles: true }));
            input.dispatchEvent(new Event("change", { bubbles: true }));
        }
    }

    function syncCustomFieldVisibility(item, showCustom) {
        var customProperty = item.querySelector("input[name*='customProperty'], coral-textfield[name*='customProperty']");
        var container = customProperty ? customProperty.closest("div") : null;

        if (container) {
            container.style.display = showCustom ? "block" : "none";
        }
    }

    function suggestFieldTypeForMapping(item, mappingValue) {

        var fieldType = item.querySelector("coral-select[name*='fieldType']");

        if (!fieldType || MULTI_VALUE_AEM_FIELDS.indexOf(mappingValue) === -1) {
            return;
        }

        Coral.commons.ready(fieldType, function () {

            if (!fieldType.value || fieldType.value === "text") {
                fieldType.value = "strings";
                fieldType.dispatchEvent(new Event("change", { bubbles: true }));
            }
        });
    }

    function initializeMultifieldItem(item) {

        SearchStaxConfigUtil.setEnabledSelect(item, false);

        var mappingType = item.querySelector("coral-select[name*='mappingType']");
        var indexFieldName = item.querySelector("input[name*='indexFieldName'], coral-textfield[name*='indexFieldName']");

        if (!mappingType) {
            return;
        }

        Coral.commons.ready(mappingType, function () {

            var value = mappingType.value;

            if (!value && mappingType.items && mappingType.items.length > 0) {
                value = mappingType.items.getAll()[0].value;
                mappingType.value = value;
            }

            handleMappingTypeChange(item, true);

            if (value === "custom") {
                return;
            }

            if (indexFieldName && !indexFieldName.value) {
                setTextFieldValue(item, "indexFieldName", formatIndexFieldName(value));
            }

            suggestFieldTypeForMapping(item, value);
        });
    }

    function initializeAllMultifieldItems() {

        $("coral-multifield-item").each(function () {
            initializeMultifieldItem(this);
        });
    }


    // ======================================================
    // POPULATE MULTIFIELD
    // ======================================================
    function populateMetadataMappings(mappings) {

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

                var mappingType = item.querySelector("coral-select[name*='mappingType']");
                var fieldType = item.querySelector("coral-select[name*='fieldType']");
                var isCustom = mapping.aemField === "custom";

                function applyLoadedValues() {
                    setTextFieldValue(
                        item,
                        "indexFieldName",
                        mapping.searchStaxField || ""
                    );

                    setTextFieldValue(
                        item,
                        "customProperty",
                        isCustom ? (mapping.customProperty || "") : ""
                    );

                    syncCustomFieldVisibility(item, isCustom);

                    if (fieldType) {
                        Coral.commons.ready(fieldType, function () {
                            var type = mapping.type || "text";
                            if (type === "texts") {
                                type = "strings";
                            }
                            fieldType.value = type;
                            fieldType.dispatchEvent(new Event("change", { bubbles: true }));
                        });
                    }

                    SearchStaxConfigUtil.setEnabledSelect(item, mapping.enabled);
                }

                if (mappingType) {
                    Coral.commons.ready(mappingType, function () {
                        mappingType.value = mapping.aemField;
                        mappingType.__initTriggered = true;
                        applyLoadedValues();
                        mappingType.dispatchEvent(new Event("change", { bubbles: true }));
                    });
                } else {
                    applyLoadedValues();
                }

            }, 50);
        });
    }


    // ======================================================
    // CORE LOGIC
    // ======================================================
    function handleMappingTypeChange(item, isInitial) {

        var mappingType = item.querySelector("coral-select[name*='mappingType']");
        var customProperty = item.querySelector("input[name*='customProperty'], coral-textfield[name*='customProperty']");

        if (!mappingType) {
            return;
        }

        var value = mappingType.value;
        var isCustom = value === "custom";

        syncCustomFieldVisibility(item, isCustom);

        if (isCustom) {
            if (!isInitial) {
                setTextFieldValue(item, "customProperty", "");
                setTextFieldValue(item, "indexFieldName", "");
            }
            return;
        }

        if (!isInitial && customProperty) {
            setTextFieldValue(item, "customProperty", "");
        }

        if (isInitial) {
            return;
        }

        setTextFieldValue(item, "indexFieldName", formatIndexFieldName(value));
        suggestFieldTypeForMapping(item, value);
    }


    // ======================================================
    // USER CHANGE LISTENER (FIXED INIT HANDLING)
    // ======================================================
    $(document).on("change", "coral-select[name*='mappingType']", function () {

        var item = $(this).closest("coral-multifield-item")[0];

        if (!item) {
            return;
        }

        if (this.__initTriggered) {
            this.__initTriggered = false;
            handleMappingTypeChange(item, true);
            return;
        }

        handleMappingTypeChange(item, false);
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
