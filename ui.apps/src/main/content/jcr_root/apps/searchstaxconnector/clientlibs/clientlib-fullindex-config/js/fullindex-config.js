$(document).ready(function () {

    Coral.commons.ready(document, function () {

        $.getJSON(
            "/bin/searchstaxconnector/wizard/full-index-load",

            function (data) {

                /*
                 * Root Path
                 */
                var rootPathField =
                    $("[name='./rootPath']")[0];

                if (rootPathField &&
                        data.rootPath) {

                    rootPathField.value =
                        data.rootPath;
                }

                /*
                 * Include Paths
                 */
                if (data.includePaths &&
                        data.includePaths.length > 0) {

                    populateIncludePaths(
                        data.includePaths);
                }

                /*
                 * Exclude Paths
                 */
                if (data.excludePaths &&
                        data.excludePaths.length > 0) {

                    populateExcludePaths(
                        data.excludePaths);
                }
            }

        ).fail(function (xhr) {

            console.error(
                "[FullIndex] Failed loading configuration",
                xhr);
        });
    });

    $(document).on(
    "click",
    "button[type='submit']",
    function () {

        var includePaths = [];

        $("coral-multifield")[0]
            .items
            .getAll()
            .forEach(function(item) {

                includePaths.push({

                    path:
                        item.querySelector(
                            "[name='./path']")
                            .value,

                    includeChildPath:
                        item.querySelector(
                            "[name='./includeChildPath']")
                            .checked
                });
            });

        $("[name='./includePathsJson']")
            .val(
                JSON.stringify(
                    includePaths));
    });

    var SAVE_PATH = "/bin/searchstaxconnector/wizard/fullindex-config-save";

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
            "Full Index configuration saved successfully.",
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

        var message =
            "Unable to save configuration.";

        try {

            var response =
                JSON.parse(
                    xhr.responseText);

            if (response.message) {

                message =
                    response.message;
            }

        } catch (e) {

            console.error(
                "[FullIndex] Failed parsing error response",
                e);
        }

        var ui =
            $(window).adaptTo(
                "foundation-ui");

        ui.alert(
            "Save Failed",
            message,
            "error");
    });
});

/*
 * Include Paths Multifield
 */
function populateIncludePaths(
        includePaths) {

    var multifield =
        $("coral-multifield")[0];

    if (!multifield) {

        console.warn(
            "[FullIndex] Include Paths multifield not found");

        return;
    }

    includePaths.forEach(function (
            includePath,
            index) {

        multifield.items.add();

        setTimeout(function () {

            var items =
                multifield.items.getAll();

            var item =
                items[index];

            if (!item) {
                return;
            }

            var pathField =
                item.querySelector(
                    "[name='./path']");

            if (pathField) {

                pathField.value =
                    includePath.path;
            }

            var checkbox =
                item.querySelector(
                    "[name='./includeChildPath']");
            
            if (checkbox) {

                checkbox.checked =
                    includePath.includeChildPath;
            }

        }, 200);
    });
}

/*
 * Exclude Paths Multifield
 */
function populateExcludePaths(
        values) {

    var multifield =
        $("coral-multifield")[1];

    if (!multifield) {

        console.warn(
            "[FullIndex] Exclude Paths multifield not found");

        return;
    }

    values.forEach(function (
            value,
            index) {

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
                    "[name='./excludePaths']");

            if (!field) {
                return;
            }

            field.value =
                value;

        }, 200);
    });
}