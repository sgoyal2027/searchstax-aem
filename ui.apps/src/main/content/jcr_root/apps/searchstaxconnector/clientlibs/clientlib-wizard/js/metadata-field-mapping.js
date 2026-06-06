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

    function initializeMultifieldItem(item) {

        var mappingType = item.querySelector("coral-select[name*='mappingType']");
        var customProperty = item.querySelector("input[name*='customProperty']");
        var indexFieldName = item.querySelector("input[name*='indexFieldName']");

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

            if (customProperty && !customProperty.value) {
                customProperty.value = value;
            }

            if (indexFieldName && !indexFieldName.value) {
                indexFieldName.value = formatIndexFieldName(value);

                indexFieldName.dispatchEvent(new Event("input", { bubbles: true }));
                indexFieldName.dispatchEvent(new Event("change", { bubbles: true }));
            }
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
                var customProperty = item.querySelector("input[name*='customProperty']");
                var indexFieldName = item.querySelector("input[name*='indexFieldName']");
                var enabled = item.querySelector("input[type='checkbox']");

                // CUSTOM PROPERTY
                if (customProperty) {
                    customProperty.value = mapping.customProperty || "";
                    customProperty.dispatchEvent(new Event("change", { bubbles: true }));
                }

                // INDEX FIELD (BACKEND VALUE)
                if (indexFieldName) {
                    indexFieldName.value = mapping.searchStaxField || "";

                    indexFieldName.dispatchEvent(
                        new Event("input", { bubbles: true })
                    );
                }

                // AEM FIELD DROPDOWN
                if (mappingType) {

                    Coral.commons.ready(mappingType, function () {

                        mappingType.value = mapping.aemField;

                        mappingType.__initTriggered = true;

                        mappingType.dispatchEvent(
                            new Event("change", { bubbles: true })
                        );
                    });
                }

                // FIELD TYPE DROPDOWN
                if (fieldType) {

                    Coral.commons.ready(fieldType, function () {

                        fieldType.value = mapping.type || "string";

                        fieldType.dispatchEvent(
                            new Event("change", { bubbles: true })
                        );
                    });
                }

                // ENABLED
                if (enabled) {
                    enabled.checked = mapping.enabled;
                }

            }, 50);
        });
    }


    // ======================================================
    // CORE LOGIC
    // ======================================================
    function handleMappingTypeChange(item, isInitial) {

        var mappingType = item.querySelector("coral-select[name*='mappingType']");
        var customProperty = item.querySelector("input[name*='customProperty']");
        var indexFieldName = item.querySelector("input[name*='indexFieldName']");

        if (!mappingType) return;

        var value = mappingType.value;

        var container = customProperty
            ? customProperty.closest("div")
            : null;

        // CUSTOM CASE
        if (value === "custom") {

            if (container) container.style.display = "block";

            if (customProperty) customProperty.value = "";
            if (indexFieldName) indexFieldName.value = "";

            return;
        }

        if (container) container.style.display = "none";

        if (customProperty) {
            customProperty.value = value;
        }

        // INIT MODE → DO NOTHING
        if (isInitial) {
            return;
        }

        // USER MODE → APPLY TRANSFORM
        var formattedValue = formatIndexFieldName(value);

        if (indexFieldName) {

            indexFieldName.value = formattedValue;

            indexFieldName.dispatchEvent(new Event("input", { bubbles: true }));
            indexFieldName.dispatchEvent(new Event("change", { bubbles: true }));
        }
    }


    // ======================================================
    // USER CHANGE LISTENER (FIXED INIT HANDLING)
    // ======================================================
    $(document).on("change", "coral-select[name*='mappingType']", function () {

        var item = $(this).closest("coral-multifield-item")[0];

        if (!item) {
            return;
        }

        // ignore init-trigger events
        if (this.__initTriggered) {
            this.__initTriggered = false;
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