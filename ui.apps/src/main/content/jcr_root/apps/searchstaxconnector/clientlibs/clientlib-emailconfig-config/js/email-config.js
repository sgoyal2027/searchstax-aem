$(document).ready(function () {

    if (!window.location.pathname.includes("emailconfiguration")) {
        return;
    }

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

    var TEST_PATH = "/bin/searchstaxconnector/wizard/email-config-test";
    var SAVE_PATH = "/bin/searchstaxconnector/wizard/email-config-save";

    Coral.commons.ready(document, function () {

        $.getJSON(
            "/bin/searchstaxconnector/wizard/email-config-load",
            function (data) {
                textFields.forEach(function (fieldName) {
                    setFieldValue(fieldName, data[fieldName]);
                });

                if (data.smtpPort) {
                    setFieldValue("smtpPort", data.smtpPort);
                }

                checkboxFields.forEach(function (fieldName) {
                    setCheckboxValue(fieldName, Boolean(data[fieldName]));
                });
            }
        ).fail(function (xhr) {
            console.error("[EmailConfig] Failed loading configuration", xhr);
        });

        bindTestEmailButton();
    });

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
            setTestResult(result.data.message || (success ? "Test email sent." : "Test email failed."), !success);
        }).catch(function (error) {
            setTestResult("Unable to reach test email endpoint: " + error.message, true);
        }).finally(function () {
            button.disabled = false;
        });
    }

    function isSaveRequest(requestUrl, servletPath) {
        if (!requestUrl) {
            return false;
        }
        return requestUrl.split("?")[0].endsWith(servletPath);
    }

    $(document).ajaxSuccess(function (event, xhr, settings) {
        if (!isSaveRequest(settings.url, SAVE_PATH)) {
            return;
        }

        var ui = $(window).adaptTo("foundation-ui");
        ui.notify("Success", "Email configuration saved successfully.", "success");

        setTimeout(function () {
            window.location.href = "/aem/start.html";
        }, 1500);
    });

    $(document).ajaxError(function (event, xhr, settings) {
        if (!isSaveRequest(settings.url, SAVE_PATH)) {
            return;
        }

        var message = "Unable to save email configuration.";
        try {
            var response = JSON.parse(xhr.responseText);
            if (response.message) {
                message = response.message;
            }
        } catch (e) {
            console.error("[EmailConfig] Failed parsing error response", e);
        }

        var ui = $(window).adaptTo("foundation-ui");
        ui.alert("Save Failed", message, "error");
    });
});

function setFieldValue(name, value) {
    var field = document.querySelector("[name='" + name + "']");
    if (!field) {
        return;
    }
    Coral.commons.ready(field, function (el) {
        if (el.value !== undefined) {
            el.value = value || "";
        }
    });
}

function setCheckboxValue(name, checked) {
    var field = document.querySelector("[name='" + name + "']");
    if (!field) {
        return;
    }
    Coral.commons.ready(field, function (el) {
        if (el.checked !== undefined) {
            el.checked = checked;
        }
    });
}
