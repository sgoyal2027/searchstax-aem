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
    });

    function isSaveRequest(requestUrl, servletPath) {
        if (!requestUrl) {
            return false;
        }
        return requestUrl.split("?")[0].endsWith(servletPath);
    }

    var SAVE_PATH = "/bin/searchstaxconnector/wizard/email-config-save";

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
