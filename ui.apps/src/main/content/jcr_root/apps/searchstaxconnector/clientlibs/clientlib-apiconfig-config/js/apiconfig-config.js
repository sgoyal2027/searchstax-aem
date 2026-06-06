$(document).ready(function () {

    if (!window.location.pathname.includes("apiconfig")) {
        return;
    }

    var fields = [
        "endpointUrl",
        "selectEndpoint",
        "updateEndpoint",
        "autoSuggestApi",
        "relatedSearchesEndpoint",
        "popularSearchesEndpoint",
        "analyticsTrackingUrl",
        "analyticsReportingUrl",
        "forwardGeocodingEndpoint",
        "reverseGeocodingEndpoint"
    ];

    Coral.commons.ready(document, function () {

        $.getJSON(
            "/bin/searchstaxconnector/wizard/api-config-load",
            function (data) {
                fields.forEach(function (fieldName) {
                    setFieldValue(fieldName, data[fieldName]);
                });
            }
        ).fail(function (xhr) {
            console.error("[ApiConfig] Failed loading configuration", xhr);
        });
    });

    function isSaveRequest(requestUrl, servletPath) {
        if (!requestUrl) {
            return false;
        }
        return requestUrl.split("?")[0].endsWith(servletPath);
    }

    var SAVE_PATH = "/bin/searchstaxconnector/wizard/api-config-save";

    $(document).ajaxSuccess(function (event, xhr, settings) {
        if (!isSaveRequest(settings.url, SAVE_PATH)) {
            return;
        }

        var ui = $(window).adaptTo("foundation-ui");
        ui.notify("Success", "API configuration saved successfully.", "success");

        setTimeout(function () {
            window.location.href = "/aem/start.html";
        }, 1500);
    });

    $(document).ajaxError(function (event, xhr, settings) {
        if (!isSaveRequest(settings.url, SAVE_PATH)) {
            return;
        }

        var message = "Unable to save API configuration.";
        try {
            var response = JSON.parse(xhr.responseText);
            if (response.message) {
                message = response.message;
            }
        } catch (e) {
            console.error("[ApiConfig] Failed parsing error response", e);
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
