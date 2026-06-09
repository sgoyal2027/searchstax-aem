(function (document, $) {
    "use strict";

    if (!window.location.pathname.includes("emailconfiguration")) {
        return;
    }

    var LOG = "[EmailConfig]";
    var LOAD_PATH = "/bin/searchstaxconnector/wizard/email-config-load";
    var TEST_PATH = "/bin/searchstaxconnector/wizard/email-config-test";
    var SAVE_PATH = "/bin/searchstaxconnector/wizard/email-config-save";

    var textFields = [
        "smtpHost",
        "smtpUser",
        "fromEmail",
        "receiverEmails"
    ];

    var checkboxFields = [
        "smtpUseSSL",
        "smtpUseStartTLS",
        "notifyOnIndexingFailure"
    ];

    function findField(name) {
        var candidates = [name, "./" + name];
        var selectors = [];

        candidates.forEach(function (fieldName) {
            selectors.push("coral-textfield[name='" + fieldName + "']");
            selectors.push("coral-password[name='" + fieldName + "']");
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
    }

    function setFieldValue(name, value) {
        var field = findField(name);
        if (!field) {
            console.warn(LOG, "Field not found:", name);
            return;
        }

        var normalized = value === undefined || value === null ? "" : String(value);

        Coral.commons.ready(field, function (el) {
            if (el.value !== undefined) {
                el.value = normalized;
            }

            var inner = el.querySelector("input:not([type='hidden']), textarea");
            if (inner) {
                inner.value = normalized;
            }

            el.dispatchEvent(new Event("change", { bubbles: true }));
        });
    }

    function setCheckboxValue(name, checked) {
        var field = findField(name);
        if (!field) {
            console.warn(LOG, "Checkbox not found:", name);
            return;
        }

        Coral.commons.ready(field, function (el) {
            if (el.checked !== undefined) {
                el.checked = checked;
            }

            var inner = el.querySelector("input[type='checkbox']");
            if (inner) {
                inner.checked = checked;
            }

            if (checked) {
                el.setAttribute("checked", "checked");
            } else {
                el.removeAttribute("checked");
            }

            el.dispatchEvent(new Event("change", { bubbles: true }));
        });
    }

    function populateForm(data) {
        if (!data) {
            return false;
        }

        var hostField = findField("smtpHost");
        if (!hostField) {
            return false;
        }

        textFields.forEach(function (fieldName) {
            setFieldValue(fieldName, data[fieldName]);
        });

        if (data.smtpPort !== undefined && data.smtpPort !== null) {
            setFieldValue("smtpPort", data.smtpPort);
        }

        checkboxFields.forEach(function (fieldName) {
            setCheckboxValue(fieldName, Boolean(data[fieldName]));
        });

        applySecretFieldStates(data);

        return true;
    }

    function applySecretFieldStates(data) {
        if (!data || !window.SearchStaxConfigUtil || !window.SearchStaxConfigUtil.applySecretFieldStates) {
            return;
        }

        window.SearchStaxConfigUtil.applySecretFieldStates(data, ["smtpPassword"], 0);
    }

    function loadConfiguration(attempt) {
        attempt = attempt || 0;

        $.getJSON(LOAD_PATH, function (data) {
            if (!populateForm(data) && attempt < 10) {
                setTimeout(function () {
                    loadConfiguration(attempt + 1);
                }, 200);
            }
        }).fail(function (xhr) {
            console.error(LOG, "Failed loading configuration", xhr);
        });
    }

    function bindTestEmailButton() {
        var button = document.querySelector("[data-granite-id='searchstax-email-test-button']")
            || document.getElementById("searchstax-email-test-button");

        if (!button || button.dataset.searchstaxEmailTestBound === "true") {
            return;
        }

        button.dataset.searchstaxEmailTestBound = "true";
        button.addEventListener("click", function (event) {
            event.preventDefault();
            sendTestEmail(button);
        });
    }

    function getCsrfToken() {
        return fetch("/libs/granite/csrf/token.json", { credentials: "same-origin" })
            .then(function (response) {
                return response.ok ? response.json() : null;
            })
            .then(function (data) {
                return data && data.token ? data.token : "";
            })
            .catch(function () {
                return "";
            });
    }

    function setTestResult(message, isError) {
        var result = document.querySelector("[data-granite-id='searchstax-email-test-result']")
            || document.getElementById("searchstax-email-test-result");

        if (!result) {
            return;
        }

        result.textContent = message || "";
        result.style.color = isError ? "#c9252d" : "#12805c";
        result.style.marginTop = "8px";
    }

    function sendTestEmail(button) {
        button.disabled = true;
        setTestResult("Sending test email using saved SMTP configuration...", false);

        getCsrfToken().then(function (csrfToken) {
            var headers = {
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
            };

            if (csrfToken) {
                headers["CSRF-Token"] = csrfToken;
            }

            return fetch(TEST_PATH, {
                method: "POST",
                headers: headers,
                credentials: "same-origin"
            });
        }).then(function (response) {
            return response.text().then(function (text) {
                var data = {};

                try {
                    data = JSON.parse(text);
                } catch (error) {
                    data = { success: false, message: "Unexpected response from server." };
                }

                return { ok: response.ok, data: data };
            });
        }).then(function (result) {
            var success = result.ok && Boolean(result.data.success);
            setTestResult(
                result.data.message || (success ? "Test email sent." : "Test email failed."),
                !success
            );
        }).catch(function (error) {
            setTestResult("Unable to reach test email endpoint: " + error.message, true);
        }).finally(function () {
            button.disabled = false;
        });
    }

    function init() {
        Coral.commons.ready(document, function () {
            loadConfiguration(0);
            bindTestEmailButton();
        });

        if (window.SearchStaxConfigUtil && window.SearchStaxConfigUtil.attachSaveHandlers) {
            window.SearchStaxConfigUtil.attachSaveHandlers(
                SAVE_PATH,
                "Email configuration saved successfully."
            );
        }
    }

    $(document).ready(init);

    document.addEventListener("foundation-contentloaded", function () {
        loadConfiguration(0);
        bindTestEmailButton();
    });

})(document, window.Granite && Granite.$ ? Granite.$ : window.jQuery);
