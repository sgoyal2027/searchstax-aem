(function (window) {
    "use strict";

    window.SearchStaxConfigUtil = window.SearchStaxConfigUtil || {};

    window.SearchStaxConfigUtil.isServletRequest = function (requestUrl, servletPath) {
        if (!requestUrl || !servletPath) {
            return false;
        }

        var path = String(requestUrl).split("?")[0];
        return path === servletPath;
    };

    window.SearchStaxConfigUtil.parseErrorMessage = function (xhr, defaultMessage) {
        if (!xhr) {
            return defaultMessage;
        }

        var contentType = xhr.getResponseHeader && xhr.getResponseHeader("Content-Type");
        var responseText = xhr.responseText || "";

        if (contentType && contentType.indexOf("json") !== -1 && responseText) {
            try {
                var response = JSON.parse(responseText);
                if (response.message) {
                    return response.message;
                }
            } catch (e) {
                // fall through
            }
        }

        if (xhr.status === 404) {
            return "Configuration service not found. Redeploy the SearchStax connector package.";
        }

        if (xhr.status === 403) {
            return "Access denied. Check your permissions or CSRF token and try again.";
        }

        if (xhr.status >= 500) {
            return defaultMessage + " (HTTP " + xhr.status + "). Check AEM error logs.";
        }

        return defaultMessage;
    };

    window.SearchStaxConfigUtil.isTruthyEnabled = function (enabled) {
        return enabled === true || enabled === "true" || enabled === 1 || enabled === "1";
    };

    window.SearchStaxConfigUtil.toEnabledValue = function (enabled) {
        return window.SearchStaxConfigUtil.isTruthyEnabled(enabled) ? "true" : "false";
    };

    window.SearchStaxConfigUtil.clearMultifield = function (multifield) {
        if (!multifield || !multifield.items) {
            return;
        }

        var items = multifield.items.getAll();
        for (var i = items.length - 1; i >= 0; i--) {
            multifield.items.remove(items[i]);
        }
    };

    window.SearchStaxConfigUtil.setEnabledSelect = function (item, enabled) {
        if (!item) {
            return;
        }

        var enabledSelect = item.querySelector("coral-select[name*='enabled']");
        if (!enabledSelect) {
            return;
        }

        var value = window.SearchStaxConfigUtil.toEnabledValue(enabled);
        Coral.commons.ready(enabledSelect, function () {
            enabledSelect.value = value;
            enabledSelect.dispatchEvent(new Event("change", { bubbles: true }));
        });
    };

    window.SearchStaxConfigUtil.attachSaveHandlers = function (saveUrl, successMessage) {
        $(document).ajaxSuccess(function (event, xhr, settings) {
            if (!window.SearchStaxConfigUtil.isServletRequest(settings.url, saveUrl)) {
                return;
            }

            var ui = $(window).adaptTo("foundation-ui");
            ui.notify("Success", successMessage, "success");
        });

        $(document).ajaxError(function (event, xhr, settings) {
            if (!window.SearchStaxConfigUtil.isServletRequest(settings.url, saveUrl)) {
                return;
            }

            var message = window.SearchStaxConfigUtil.parseErrorMessage(
                xhr,
                "Unable to save configuration."
            );

            var ui = $(window).adaptTo("foundation-ui");
            ui.alert("Save Failed", message, "error");
        });
    };
})(window);
