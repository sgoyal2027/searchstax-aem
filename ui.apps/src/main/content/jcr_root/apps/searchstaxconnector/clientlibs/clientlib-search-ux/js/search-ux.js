(function (document, window) {
    "use strict";

    function getSearchstaxConstructor() {
        var vendor = window["@searchstaxInc/searchstudioUxJs"];
        return vendor && vendor.Searchstax ? vendor.Searchstax : null;
    }

    function makeSessionId(length) {
        var result = "";
        var characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (var i = 0; i < length; i++) {
            result += characters.charAt(Math.floor(Math.random() * characters.length));
        }
        return result;
    }

    function fetchConfig(configUrl) {
        return fetch(configUrl, {
            method: "GET",
            credentials: "same-origin",
            headers: { Accept: "application/json" }
        }).then(function (response) {
            if (!response.ok) {
                throw new Error("Search configuration request failed (" + response.status + ")");
            }
            return response.json();
        });
    }

    function showError(root, message) {
        root.innerHTML =
            '<div class="searchstax-aem-search-error" role="alert">' +
            "<p>" + message + "</p>" +
            "<p>Configure SearchStax under <strong>Tools &gt; SearchStax Connector &gt; API Configuration</strong>.</p>" +
            "</div>";
    }

    function initializeInstance(root) {
        var Searchstax = getSearchstaxConstructor();
        if (!Searchstax) {
            showError(root, "SearchStax UX library failed to load.");
            return;
        }

        var configUrl = root.getAttribute("data-config-url");
        var renderMethod = root.getAttribute("data-render-method") || "pagination";
        var facetingType = root.getAttribute("data-faceting-type") || "and";
        var placeholder = root.getAttribute("data-placeholder") || "Search...";
        var componentId = root.getAttribute("data-component-id") || "searchstax-search";

        fetchConfig(configUrl)
            .then(function (config) {
                if (!config || !config.enabled) {
                    showError(root, (config && config.message) || "Search is not configured.");
                    return;
                }

                var searchstax = new Searchstax();
                searchstax.initialize({
                    language: config.language || "en",
                    searchURL: config.searchURL,
                    suggesterURL: config.suggesterURL,
                    searchAuth: config.searchAuth,
                    authType: config.authType || "token",
                    trackApiKey: config.trackApiKey,
                    relatedSearchesURL: config.relatedSearchesURL,
                    relatedSearchesAPIKey: config.relatedSearchesAPIKey,
                    analyticsBaseUrl: config.analyticsBaseUrl,
                    sessionId: makeSessionId(25),
                    router: { enabled: true }
                });

                var inputId = componentId + "-input";
                var resultsId = componentId + "-results";

                searchstax.addSearchInputWidget(componentId + "-input-container", {
                    templates: {
                        mainTemplate: {
                            template:
                                '<div class="searchstax-search-input-container">' +
                                '<div class="searchstax-search-input-wrapper">' +
                                '<input type="text" id="' + inputId + '" class="searchstax-search-input" ' +
                                'placeholder="' + placeholder + '" aria-label="Search" />' +
                                '<button id="searchstax-clear-input-action-button" class="searchstax-cross-icon hidden" ' +
                                'aria-label="clear input" role="button"></button>' +
                                "</div>" +
                                '<button class="searchstax-search-icon" id="searchstax-search-input-action-button" ' +
                                'aria-label="search" role="button"></button>' +
                                "</div>",
                            searchInputId: inputId
                        },
                        autosuggestItemTemplate: {
                            template: '<div class="searchstax-autosuggest-item-term-container">{{{term}}}</div>'
                        }
                    }
                });

                searchstax.addFacetsWidget(componentId + "-facets-container", {
                    facetingType: facetingType,
                    itemsPerPageDesktop: 5,
                    itemsPerPageMobile: 99,
                    templates: {
                        mainTemplateDesktop: {
                            template:
                                "{{#hasResultsOrExternalPromotions}}" +
                                '<div class="searchstax-facets-container-desktop"></div>' +
                                "{{/hasResultsOrExternalPromotions}}",
                            facetsContainerClass: "searchstax-facets-container-desktop",
                            selectedFacetsContainerClass: "searchstax-facets-pills-selected"
                        },
                        facetItemContainerTemplate: {
                            template:
                                "<div>" +
                                '<div class="searchstax-facet-title-container">' +
                                '<div class="searchstax-facet-title" aria-label="Facet group: {{label}}" tabindex="0" role="button">{{label}}</div>' +
                                '<div class="searchstax-facet-title-arrow active"></div>' +
                                "</div>" +
                                '<div class="searchstax-facet-values-container"></div>' +
                                "</div>",
                            facetListTitleContainerClass: "searchstax-facet-title-container",
                            facetListTitleContainerInner: "searchstax-facet-title",
                            facetListContainerClass: "searchstax-facet-values-container"
                        }
                    }
                });

                searchstax.addSearchFeedbackWidget(componentId + "-feedback-container", {
                    templates: {
                        main: {
                            template:
                                "{{#searchExecuted}}" +
                                '<h2 class="searchstax-feedback-container" aria-live="polite">' +
                                "{{#hasResults}}" +
                                "Showing <b>{{startResultIndex}} - {{endResultIndex}}</b> of <b>{{totalResults}}</b> results" +
                                '{{#searchTerm}} for "<b>{{searchTerm}}</b>"{{/searchTerm}}' +
                                "{{/hasResults}}" +
                                "</h2>" +
                                "{{/searchExecuted}}",
                            originalQueryClass: "searchstax-feedback-original-query"
                        }
                    }
                });

                searchstax.addSearchResultsWidget(componentId + "-results-container", {
                    renderMethod: renderMethod,
                    templates: {
                        mainTemplate: {
                            template:
                                '<section aria-label="search results container" tabindex="0">' +
                                '<div class="searchstax-search-results-container" id="searchstax-search-results-container">' +
                                '<div class="searchstax-search-results" id="' + resultsId + '"></div>' +
                                "</div>" +
                                "</section>",
                            searchResultsContainerId: resultsId
                        },
                        searchResultTemplate: {
                            template:
                                '<a href="{{url}}" data-searchstax-unique-result-id="{{uniqueId}}" ' +
                                'class="searchstax-result-item-link searchstax-result-item-link-wrapping" tabindex="0">' +
                                '<div class="searchstax-search-result searchstax-search-result-wrapping {{#thumbnail}}has-thumbnail{{/thumbnail}}">' +
                                "{{#thumbnail}}<img alt=\"\" src=\"{{thumbnail}}\" class=\"searchstax-thumbnail\">{{/thumbnail}}" +
                                '<div class="searchstax-search-result-title-container">' +
                                '<h3 class="searchstax-search-result-title">{{{title}}}</h3>' +
                                "</div>" +
                                "{{#description}}" +
                                '<p class="searchstax-search-result-description searchstax-search-result-common">{{{description}}}</p>' +
                                "{{/description}}" +
                                "</div></a>",
                            searchResultUniqueIdAttribute: "data-searchstax-unique-result-id"
                        },
                        noSearchResultTemplate: {
                            template:
                                "{{#searchExecuted}}" +
                                '<div class="searchstax-no-results-wrap">' +
                                '<div class="searchstax-no-results">Showing <strong>no results</strong> for <strong>"{{searchTerm}}"</strong></div>' +
                                "</div>{{/searchExecuted}}"
                        }
                    }
                });

                searchstax.addPaginationWidget(componentId + "-pagination-container", {
                    templates: {
                        mainTemplate: {
                            template:
                                "{{#results.length}}" +
                                '<div class="searchstax-pagination-container">' +
                                '<div class="searchstax-pagination-content">' +
                                '<a role="link" class="searchstax-pagination-previous {{#isFirstPage}}disabled{{/isFirstPage}}" ' +
                                'id="searchstax-pagination-previous">Previous</a>' +
                                '<div class="searchstax-pagination-details">{{startResultIndex}} - {{endResultIndex}} of {{totalResults}}</div>' +
                                '<a role="link" class="searchstax-pagination-next {{#isLastPage}}disabled{{/isLastPage}}" ' +
                                'id="searchstax-pagination-next">Next</a>' +
                                "</div></div>{{/results.length}}",
                            nextButtonClass: "searchstax-pagination-next",
                            previousButtonClass: "searchstax-pagination-previous"
                        },
                        infiniteScrollTemplate: {
                            template:
                                "{{#results.length}}{{^isLastPage}}" +
                                '<a class="searchstax-pagination-load-more" tabindex="0">Show more</a>' +
                                "{{/isLastPage}}{{/results.length}}",
                            loadMoreButtonClass: "searchstax-pagination-load-more"
                        }
                    }
                });

                if (config.relatedSearchesURL && config.relatedSearchesAPIKey) {
                    searchstax.addRelatedSearchesWidget(componentId + "-related-container", {
                        relatedSearchesURL: config.relatedSearchesURL,
                        relatedSearchesAPIKey: config.relatedSearchesAPIKey,
                        templates: {
                            main: {
                                template:
                                    "{{#hasRelatedSearches}}" +
                                    '<div class="searchstax-related-searches-container">' +
                                    "Related searches: {{#relatedSearches}}" +
                                    '<span class="searchstax-related-search searchstax-related-search-item">{{related_search}}</span>' +
                                    "{{/relatedSearches}}</div>{{/hasRelatedSearches}}",
                                relatedSearchesContainerClass: "searchstax-related-search"
                            },
                            relatedSearch: {
                                template:
                                    '<span class="searchstax-related-search searchstax-related-search-item" ' +
                                    'aria-label="Related search: {{related_search}}" tabindex="0" role="link">' +
                                    "{{related_search}}{{^last}}, {{/last}}</span>",
                                relatedSearchContainerClass: "searchstax-related-search-item"
                            }
                        }
                    });
                }
            })
            .catch(function (error) {
                showError(root, error && error.message ? error.message : "Unable to load search configuration.");
            });
    }

    function boot() {
        var roots = document.querySelectorAll(".searchstax-aem-search[data-config-url]");
        for (var i = 0; i < roots.length; i++) {
            initializeInstance(roots[i]);
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", boot);
    } else {
        boot();
    }
})(document, window);
