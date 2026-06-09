(function (window) {
    "use strict";

    window.SearchStaxConfigUtil = window.SearchStaxConfigUtil || {};

    window.SearchStaxConfigUtil.resolveJQuery = function () {
        if (window.Granite && window.Granite.$) {
            return window.Granite.$;
        }
        return window.jQuery;
    };

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

    window.SearchStaxConfigUtil.showDuplicateWarning = function (message) {
        var ui = window.$ && $(window).adaptTo("foundation-ui");
        if (ui) {
            ui.alert("Duplicate selection", message, "warning");
        }
    };

    window.SearchStaxConfigUtil.showValidationError = function (message) {
        var ui = window.$ && $(window).adaptTo("foundation-ui");
        if (ui) {
            ui.alert("Validation Failed", message, "error");
        }
    };

    window.SearchStaxConfigUtil.attachFormValidation = function (validateBeforeSave) {
        if (typeof validateBeforeSave !== "function") {
            return;
        }

        var form = document.getElementById("edit-configuration-properties-form");
        if (!form || form.__searchstaxValidateBeforeSave) {
            return;
        }

        form.__searchstaxValidateBeforeSave = validateBeforeSave;
        form.addEventListener("submit", function (event) {
            var message = form.__searchstaxValidateBeforeSave();
            if (message) {
                event.preventDefault();
                event.stopImmediatePropagation();
                window.SearchStaxConfigUtil.showValidationError(message);
                return false;
            }
        }, true);
    };

    window.SearchStaxConfigUtil.setSelectOptionsDisabled = function (select, usedValues, currentValue) {
        if (!select) {
            return;
        }

        function apply() {
            var options = select.items ? select.items.getAll() : [];
            for (var i = 0; i < options.length; i++) {
                var option = options[i];
                var value = option.value;
                if (!value) {
                    continue;
                }
                option.disabled = usedValues[value] === true && value !== currentValue;
            }
        }

        Coral.commons.ready(select, apply);
    };

    window.SearchStaxConfigUtil.firstAvailableSelectValue = function (select) {
        if (!select || !select.items) {
            return "";
        }

        var options = select.items.getAll();
        for (var i = 0; i < options.length; i++) {
            var option = options[i];
            if (option.value && !option.disabled) {
                return option.value;
            }
        }

        return "";
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

    window.SearchStaxConfigUtil.findNamedField = function (name) {
        var candidates = [name, "./" + name];
        var selectors = [];

        candidates.forEach(function (fieldName) {
            selectors.push("input[type='password'][name='" + fieldName + "']");
            selectors.push("coral-password[name='" + fieldName + "']");
            selectors.push("coral-textfield[name='" + fieldName + "']");
            selectors.push("coral-numberinput[name='" + fieldName + "']");
            selectors.push("coral-checkbox[name='" + fieldName + "']");
            selectors.push("input[name='" + fieldName + "']");
            selectors.push("textarea[name='" + fieldName + "']");
            selectors.push("[name='" + fieldName + "']");
        });

        for (var i = 0; i < selectors.length; i++) {
            var el = document.querySelector(selectors[i]);
            if (el) {
                return el;
            }
        }

        return null;
    };

    window.SearchStaxConfigUtil.findFieldWrapper = function (field) {
        if (!field) {
            return null;
        }

        if (field.closest) {
            return field.closest(".coral-Form-fieldwrapper")
                || field.closest(".coral-Form-field")
                || field.parentNode;
        }

        return field.parentNode;
    };

    window.SearchStaxConfigUtil.updateSecretFieldLabel = function (fieldName, configured) {
        var field = window.SearchStaxConfigUtil.findNamedField(fieldName);
        var wrapper = window.SearchStaxConfigUtil.findFieldWrapper(field);
        if (!wrapper) {
            return;
        }

        var label = wrapper.querySelector(".coral-Form-fieldlabel, label.coral-Form-fieldlabel, label");
        if (!label) {
            return;
        }

        var baseLabel = label.getAttribute("data-searchstax-base-label");
        if (!baseLabel) {
            baseLabel = (label.textContent || fieldName).replace(" (saved)", "").trim();
            label.setAttribute("data-searchstax-base-label", baseLabel);
        }

        label.textContent = configured ? baseLabel + " (saved)" : baseLabel;
    };

    window.SearchStaxConfigUtil.getSecretUpdateButtonLabel = function (fieldName) {
        var lower = String(fieldName || "").toLowerCase();
        if (lower.indexOf("password") !== -1) {
            return "Update Password";
        }
        if (lower.indexOf("token") !== -1) {
            return "Update Token";
        }
        if (lower.indexOf("key") !== -1) {
            return "Update Key";
        }
        return "Update value";
    };

    window.SearchStaxConfigUtil.setSecretUpdateButtonLabel = function (button, labelText) {
        if (!button) {
            return;
        }

        if (button.label && button.label.textContent !== undefined) {
            button.label.textContent = labelText;
            return;
        }

        var label = button.querySelector("coral-button-label");
        if (label) {
            label.textContent = labelText;
            return;
        }

        button.textContent = labelText;
    };

    window.SearchStaxConfigUtil.removeSecretUpdateButton = function (fieldName) {
        var button = document.getElementById("searchstax-update-secret-" + fieldName);
        if (!button) {
            return;
        }

        var container = button.closest(".searchstax-secret-update-row");
        if (container) {
            container.remove();
            return;
        }

        button.remove();
    };

    window.SearchStaxConfigUtil.createSecretUpdateButton = function (fieldName) {
        var buttonId = "searchstax-update-secret-" + fieldName;
        var labelText = window.SearchStaxConfigUtil.getSecretUpdateButtonLabel(fieldName);
        var button;

        if (window.Coral && typeof Coral.Button === "function") {
            button = new Coral.Button();
            button.id = buttonId;
            if (Coral.Button.variant && Coral.Button.variant.SECONDARY) {
                button.variant = Coral.Button.variant.SECONDARY;
            } else {
                button.variant = "secondary";
            }
            button.label.textContent = labelText;
            return button;
        }

        button = document.createElement("button");
        button.id = buttonId;
        button.type = "button";
        button.className = "coral-Form-field coral-Button coral-Button--secondary";
        button.textContent = labelText;
        return button;
    };

    window.SearchStaxConfigUtil.setFieldValidationRequired = function (field, required) {
        if (!field) {
            return;
        }

        function apply(el) {
            var inner = el.querySelector ? el.querySelector("input, textarea") : null;

            if (required) {
                if (el.getAttribute("data-searchstax-was-required") === "true") {
                    el.setAttribute("required", "required");
                    el.removeAttribute("data-searchstax-was-required");
                }
                if (inner && el.hasAttribute("required")) {
                    inner.required = true;
                    inner.setAttribute("required", "required");
                }
                return;
            }

            if (el.hasAttribute("required")) {
                el.setAttribute("data-searchstax-was-required", "true");
                el.removeAttribute("required");
            }
            if (el.required !== undefined) {
                el.required = false;
            }
            if (inner) {
                inner.required = false;
                inner.removeAttribute("required");
            }
        }

        if (window.Coral && Coral.commons) {
            Coral.commons.ready(field, apply);
        } else {
            apply(field);
        }
    };

    window.SearchStaxConfigUtil.setFieldDisabled = function (field, disabled) {
        if (!field) {
            return;
        }

        function apply(el) {
            if (el.disabled !== undefined) {
                el.disabled = disabled;
            }
            if (el.readOnly !== undefined) {
                el.readOnly = disabled;
            }

            var inner = el.querySelector ? el.querySelector("input, textarea") : null;
            if (inner) {
                inner.disabled = disabled;
                inner.readOnly = disabled;
                if (disabled) {
                    inner.value = "";
                } else {
                    setTimeout(function () {
                        inner.focus();
                    }, 50);
                }
            }
        }

        if (window.Coral && Coral.commons) {
            Coral.commons.ready(field, apply);
        } else {
            apply(field);
        }
    };

    window.SearchStaxConfigUtil.ensureSecretUpdateButton = function (fieldName, wrapper) {
        var buttonId = "searchstax-update-secret-" + fieldName;
        var existing = document.getElementById(buttonId);
        if (existing) {
            return existing;
        }
        if (!wrapper) {
            return null;
        }

        var container = document.createElement("div");
        container.className = "searchstax-secret-update-row coral-Form-field";

        var button = window.SearchStaxConfigUtil.createSecretUpdateButton(fieldName);
        button.addEventListener("click", function (event) {
            event.preventDefault();
            var currentWrapper = window.SearchStaxConfigUtil.findFieldWrapper(
                window.SearchStaxConfigUtil.findNamedField(fieldName)
            );
            if (!currentWrapper) {
                return;
            }
            if (currentWrapper.getAttribute("data-searchstax-secret-locked") === "true") {
                window.SearchStaxConfigUtil.unlockSecretField(fieldName);
            } else {
                window.SearchStaxConfigUtil.lockSecretField(fieldName);
            }
        });

        container.appendChild(button);
        wrapper.appendChild(container);

        if (window.Coral && Coral.commons) {
            Coral.commons.ready(button, function () {
                window.SearchStaxConfigUtil.setSecretUpdateButtonLabel(
                    button,
                    window.SearchStaxConfigUtil.getSecretUpdateButtonLabel(fieldName)
                );
            });
        }

        return button;
    };

    window.SearchStaxConfigUtil.lockSecretField = function (fieldName) {
        var field = window.SearchStaxConfigUtil.findNamedField(fieldName);
        var wrapper = window.SearchStaxConfigUtil.findFieldWrapper(field);
        if (!field || !wrapper) {
            return false;
        }

        window.SearchStaxConfigUtil.setFieldDisabled(field, true);
        window.SearchStaxConfigUtil.setFieldValidationRequired(field, false);
        window.SearchStaxConfigUtil.updateSecretFieldLabel(fieldName, true);

        var button = window.SearchStaxConfigUtil.ensureSecretUpdateButton(fieldName, wrapper);
        if (button) {
            window.SearchStaxConfigUtil.setSecretUpdateButtonLabel(
                button,
                window.SearchStaxConfigUtil.getSecretUpdateButtonLabel(fieldName)
            );
            button.style.display = "";
            var row = button.closest(".searchstax-secret-update-row");
            if (row) {
                row.style.display = "";
            }
        }

        wrapper.setAttribute("data-searchstax-secret-locked", "true");
        return true;
    };

    window.SearchStaxConfigUtil.unlockSecretField = function (fieldName) {
        var field = window.SearchStaxConfigUtil.findNamedField(fieldName);
        var wrapper = window.SearchStaxConfigUtil.findFieldWrapper(field);
        if (!field || !wrapper) {
            return false;
        }

        window.SearchStaxConfigUtil.setFieldDisabled(field, false);
        window.SearchStaxConfigUtil.setFieldValidationRequired(field, true);
        window.SearchStaxConfigUtil.updateSecretFieldLabel(fieldName, false);

        var button = document.getElementById("searchstax-update-secret-" + fieldName);
        if (button) {
            window.SearchStaxConfigUtil.setSecretUpdateButtonLabel(button, "Cancel");
        }

        wrapper.setAttribute("data-searchstax-secret-locked", "false");
        return true;
    };

    window.SearchStaxConfigUtil.applySecretFieldLock = function (fieldName, configured) {
        if (!configured) {
            var openField = window.SearchStaxConfigUtil.findNamedField(fieldName);
            window.SearchStaxConfigUtil.setFieldDisabled(openField, false);
            window.SearchStaxConfigUtil.setFieldValidationRequired(openField, true);
            window.SearchStaxConfigUtil.updateSecretFieldLabel(fieldName, false);

            var openButton = document.getElementById("searchstax-update-secret-" + fieldName);
            if (openButton) {
                window.SearchStaxConfigUtil.removeSecretUpdateButton(fieldName);
            }
            return Boolean(openField);
        }

        return window.SearchStaxConfigUtil.lockSecretField(fieldName);
    };

    window.SearchStaxConfigUtil.applySecretFieldStates = function (data, fieldNames, attempt) {
        if (!data || !fieldNames || !fieldNames.length) {
            return;
        }

        attempt = attempt || 0;
        var pending = [];

        fieldNames.forEach(function (fieldName) {
            var configuredKey = fieldName + "Configured";
            if (!Object.prototype.hasOwnProperty.call(data, configuredKey)) {
                return;
            }

            var configured = window.SearchStaxConfigUtil.isTruthyEnabled(data[configuredKey]);
            if (!window.SearchStaxConfigUtil.applySecretFieldLock(fieldName, configured)) {
                pending.push(fieldName);
            }
        });

        if (pending.length && attempt < 15) {
            setTimeout(function () {
                window.SearchStaxConfigUtil.applySecretFieldStates(data, pending, attempt + 1);
            }, 200);
        }
    };

    window.SearchStaxConfigUtil.attachSaveHandlers = function (saveUrl, successMessage, validateBeforeSave) {
        var $ = window.SearchStaxConfigUtil.resolveJQuery();
        if (!$) {
            console.warn("[SearchStaxConfigUtil] jQuery is not available; save handlers not attached.");
            return;
        }

        window.SearchStaxConfigUtil.attachFormValidation(validateBeforeSave);

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
