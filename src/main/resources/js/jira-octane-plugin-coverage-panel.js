jQuery(document).ready(function() {
    load(1);
});

function load(counter) {
    var panelEl = jQuery("#octane-coverage-panel:not(.resolved)");

    if (!panelEl.length) {
        if(counter < 30) {
            setTimeout(function () {
                load(counter + 1);
            }, 150);
        }
    } else {
        console.log("[coverage] octane plugin load successfully " + counter);
        var projectKey = panelEl.attr("project-key");
        var issueKey = panelEl.attr("issue-key");
        var issueId = panelEl.attr("issue-id");
        jQuery("#octane-coverage-panel").addClass("resolved");
        loadOctaneCoverageWidget(projectKey, issueKey, issueId);
    }
}

function loadOctaneCoverageWidget(projectKey, issueKey, issueId) {
    var query = "&project-key=" + projectKey + "&issue-key=" + issueKey + "&issue-id=" + issueId;
    var url = AJS.contextPath() + "/rest/octane-coverage/1.0/coverage?" + query;
    console.log("[coverage] loadOctaneCoverageWidget :" + url);

    //do request
    jQuery.ajax({
        url: url, type: "GET", dataType: "json", contentType: "application/json"
    }).done(function (data) {
        var panelEl = jQuery("#octane-coverage-panel");
        var issueKeyFromHtml = panelEl.attr("issue-key");
        var issueKeyFromData = data.issueKey;
        if (issueKeyFromHtml !== issueKeyFromData) {
            console.log("[coverage] issueKeyFromHtml(" + issueKeyFromHtml + ")!==issueKeyFromData(" + issueKeyFromData + ") => return");
            return;
        }

        jQuery("#octane-loading-section").addClass("hidden");

        if (data.status === 'noData') {
            jQuery("#octane-no-data-section").removeClass("hidden");
        } else if (data.status === 'noValidConfiguration') {
            jQuery("#octane-no-valid-configuration-section").removeClass("hidden");
        } else if (data.status === 'hasData') {
            jQuery("#octane-entity-section").removeClass("hidden");

            //entity settings
            var octaneEntity = data.octaneEntity.fields;
            jQuery("#octane-entity-icon-text").text(octaneEntity.typeAbbreviation);
            jQuery("#octane-entity-icon").css("background-color", octaneEntity.typeColor);
            jQuery("#octane-entity-url a").attr("href", octaneEntity.url);
            jQuery("#octane-entity-url a").text(octaneEntity.id);
            jQuery("#octane-entity-name").text(octaneEntity.name);
            jQuery("#octane-entity-name").attr("title", octaneEntity.name);

            //totals
            var totalRuns = data.totalExecutedTestsCount ? data.totalExecutedTestsCount + " last runs:" : "No last runs";
            jQuery("#octane-total-runs").text(totalRuns);
            if (data.totalTestsCount) {
                jQuery("#octane-total-tests").text(data.totalTestsCount);
                jQuery("#view-tests-in-alm").attr("href", octaneEntity.testTabUrl);

            } else {
                jQuery("#view-tests-in-alm").text("No linked tests in ALM Octane");
                jQuery("#view-tests-in-alm").attr("style", "pointer-events: none; cursor: default; color: gray; font-weight: bold");
            }

            //coverage groups
            data.coverageGroups.forEach(function (entry) {
                var idSelector = "#" + entry.fields.id;
                var countSelector = idSelector + " .octane-test-status-count";
                var percentageSelector = idSelector + " .octane-test-status-percentage";
                jQuery(idSelector).removeClass("hidden");
                jQuery(countSelector).text(entry.fields.countStr);
                jQuery(percentageSelector).text(entry.fields.percentage);
            });
        }

        if (data.debug) {
            jQuery("#octane-debug-section").removeClass("hidden");
            jQuery.each(data.debug, function (key, val) {
                jQuery("#octane-debug-section").append("<p>" + key + " : " + val + "</p>");
            });
        }

    }).fail(function (request, status, error) {
        console.log(request.responseText);
    });
}