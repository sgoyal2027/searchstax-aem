$(document).ready(function () {

    if (!window.location.pathname.includes("initialsetup")) {
        return;
    }

    Coral.commons.ready(document, function () {

        $.getJSON(
            "/bin/searchstaxconnector/wizard/initial-setup-load",

            function (data) {

                /*
                 * Enable Connector
                 */
                $("input[name='./enableConnector']")
                    .prop(
                        "checked",
                        data.enableConnector);

                /*
                 * Root Paths
                 */
                if (data.rootPaths &&
                        data.rootPaths.length > 0) {

                    populateMultifield(
                        "rootPaths",
                        data.rootPaths,
                        0);
                }

                /*
                 * Exclude Paths
                 */
                if (data.excludePaths &&
                        data.excludePaths.length > 0) {

                    populateMultifield(
                        "excludePaths",
                        data.excludePaths,
                        1);
                }

                /*
                 * Allowed Files
                 */
                if (data.allowedFiles &&
                        data.allowedFiles.length > 0) {

                    var allowedFilesField =
                        document.querySelector(
                            "[name='./allowedFiles']");

                    if (allowedFilesField) {

                        Coral.commons.ready(
                            allowedFilesField,

                            function (select) {

                                select.values =
                                    data.allowedFiles;
                            });
                    }
                }
            }
        ).fail(function (xhr) {

            console.error(
                "[InitialSetup] Failed loading configuration",
                xhr);
        });
    });

    var SAVE_PATH = "/bin/searchstaxconnector/wizard/initial-setup-config";

    function isSaveRequest(requestUrl, servletPath) {
        if (!requestUrl) {
            return false;
        }
        return requestUrl.split("?")[0].endsWith(servletPath);
    }

    /*
     * Success handler
     */
    $(document).ajaxSuccess(function (
            event,
            xhr,
            settings) {

        if (!isSaveRequest(settings.url, SAVE_PATH)) {

            return;
        }

        var ui =
            $(window).adaptTo(
                "foundation-ui");

        ui.notify(
            "Success",
            "Initial configuration saved successfully.",
            "success");

        setTimeout(function () {

            window.location.href =
                "/aem/start.html";

        }, 1500);
    });

    /*
     * Error handler
     */
    $(document).ajaxError(function (
            event,
            xhr,
            settings) {

        if (!isSaveRequest(settings.url, SAVE_PATH)) {

            return;
        }

        var message = parseSaveErrorMessage(
            xhr,
            "Unable to save configuration."
        );

        var ui =
            $(window).adaptTo(
                "foundation-ui");

        ui.alert(
            "Save Failed",
            message,
            "error");
    });
});

function parseSaveErrorMessage(xhr, defaultMessage) {
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
}

function populateMultifield(
        fieldName,
        values,
        multifieldIndex) {

    var multifield =
        $("coral-multifield")[multifieldIndex];

    if (!multifield) {

        console.warn(
            "[InitialSetup] Multifield not found:",
            fieldName);

        return;
    }

    values.forEach(function (value, index) {

        multifield.items.add();

        setTimeout(function () {

            var items =
                multifield.items.getAll();

            var item =
                items[index];

            if (!item) {
                return;
            }

            var field =
                item.querySelector(
                    "[name='./" + fieldName + "']");

            if (!field) {
                return;
            }

            field.value = value;

        }, 200);
    });
}