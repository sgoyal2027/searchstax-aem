(function ($, document) {
    "use strict";

    if (!window.location.pathname.includes("/indexingreport")) {
        return;
    }

    var REFRESH_INTERVAL_MS = 60000;
    var REPORT_URL = "/bin/searchstaxconnector/wizard/indexing-report";
    var TOOLBAR_ID = "searchstax-indexing-report-toolbar";
    var EMPTY_ID = "searchstax-indexing-report-empty";
    var STATUS_ORDER = ["QUEUED", "SUCCESS", "FAILURE", "SKIPPED"];

    var allRecords = [];
    var activeFilter = "ALL";
    var refreshTimer = null;
    var initialized = false;

    function formatTimestamp(value) {
        if (!value) {
            return "";
        }
        try {
            return new Date(value).toLocaleString();
        } catch (error) {
            return value;
        }
    }

    function statusVariant(status) {
        if (status === "SUCCESS") {
            return "success";
        }
        if (status === "FAILURE") {
            return "error";
        }
        if (status === "QUEUED") {
            return "info";
        }
        if (status === "SKIPPED") {
            return "warning";
        }
        return "quiet";
    }

    function findReportTable() {
        return document.querySelector('[data-granite-id="searchstax-indexing-report-table"]')
            || document.getElementById("searchstax-indexing-report-table");
    }

    function findTableAnchor() {
        var table = findReportTable();
        return table ? table.parentElement : null;
    }

    function countByStatus(records) {
        var counts = {
            ALL: records.length,
            QUEUED: 0,
            SUCCESS: 0,
            FAILURE: 0,
            SKIPPED: 0
        };

        (records || []).forEach(function (record) {
            if (counts[record.status] !== undefined) {
                counts[record.status]++;
            }
        });

        return counts;
    }

    function ensureToolbar(anchor) {
        var toolbar = document.getElementById(TOOLBAR_ID);
        if (!toolbar && anchor) {
            toolbar = document.createElement("div");
            toolbar.id = TOOLBAR_ID;
            toolbar.className = "searchstax-indexing-report-toolbar";
            anchor.insertBefore(toolbar, findReportTable());
        }
        return toolbar;
    }

    function ensureEmptyMessage(anchor) {
        var empty = document.getElementById(EMPTY_ID);
        if (!empty && anchor) {
            empty = document.createElement("div");
            empty.id = EMPTY_ID;
            empty.className = "searchstax-indexing-report-empty";
            anchor.appendChild(empty);
        }
        return empty;
    }

    function renderToolbar(counts) {
        var anchor = findTableAnchor();
        var toolbar = ensureToolbar(anchor);
        if (!toolbar) {
            return;
        }

        var summary = document.createElement("div");
        summary.className = "searchstax-indexing-report-summary";

        ["ALL"].concat(STATUS_ORDER).forEach(function (status) {
            var button = document.createElement("button");
            button.type = "button";
            button.setAttribute("data-status", status);
            button.className = activeFilter === status ? "is-active" : "";
            button.textContent = (status === "ALL" ? "All" : status) + " (" + (counts[status] || 0) + ")";
            button.addEventListener("click", function () {
                activeFilter = status;
                renderToolbar(counts);
                renderRows(allRecords);
            });
            summary.appendChild(button);
        });

        toolbar.innerHTML = "";
        toolbar.appendChild(summary);

        var label = document.createElement("span");
        label.className = "searchstax-indexing-report-filter-label";
        label.textContent = "Click a status to filter the table.";
        toolbar.appendChild(label);
    }

    function filterRecords(records) {
        if (activeFilter === "ALL") {
            return records || [];
        }
        return (records || []).filter(function (record) {
            return record.status === activeFilter;
        });
    }

    function decorateStatusCells(table, records) {
        var rows = table.querySelectorAll("tbody coral-table-row, tbody tr[is='coral-table-row']");
        rows.forEach(function (row, index) {
            var record = records[index];
            if (!record) {
                return;
            }

            var cells = row.querySelectorAll("coral-table-cell, td[is='coral-table-cell']");
            if (cells.length < 4) {
                return;
            }

            var statusCell = cells[3];
            statusCell.textContent = "";
            var badge = document.createElement("coral-status");
            badge.setAttribute("variant", statusVariant(record.status));
            badge.textContent = record.status || "";
            statusCell.appendChild(badge);
        });
    }

    function renderRows(records) {
        var table = findReportTable();
        var anchor = findTableAnchor();
        if (!table) {
            return;
        }

        allRecords = records || [];
        var counts = countByStatus(allRecords);
        renderToolbar(counts);

        var visibleRecords = filterRecords(allRecords);
        var empty = ensureEmptyMessage(anchor);

        Coral.commons.ready(table, function () {
            table.items.clear();

            if (!visibleRecords.length) {
                if (empty) {
                    if (!allRecords.length) {
                        empty.textContent = "No indexing events in the last 24 hours. Publish or unpublish content under configured root paths to see activity.";
                    } else {
                        empty.textContent = "No " + activeFilter + " events in the last 24 hours.";
                    }
                }
                return;
            }

            if (empty) {
                empty.textContent = "";
            }

            visibleRecords.forEach(function (record) {
                table.items.add({
                    timestamp: formatTimestamp(record.timestamp),
                    path: record.path || "",
                    action: record.action || "",
                    status: record.status || "",
                    duration: String(record.durationMs != null ? record.durationMs : ""),
                    message: record.message || ""
                });
            });

            window.requestAnimationFrame(function () {
                decorateStatusCells(table, visibleRecords);
            });
        });
    }

    function loadReport() {
        $.getJSON(REPORT_URL)
            .done(function (data) {
                renderRows(data || []);
            })
            .fail(function () {
                renderRows([]);
            });
    }

    function init() {
        if (initialized || !findReportTable()) {
            return;
        }
        initialized = true;
        loadReport();
        if (refreshTimer) {
            window.clearInterval(refreshTimer);
        }
        refreshTimer = window.setInterval(loadReport, REFRESH_INTERVAL_MS);
    }

    $(document).ready(init);
    $(document).on("foundation-contentloaded", init);
})(Granite.$, document);
