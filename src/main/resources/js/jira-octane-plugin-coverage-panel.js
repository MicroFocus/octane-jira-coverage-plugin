function loadOctaneCoverageWidget(projectKey, issueKey, issueId) {
    var query = "&project-key=" + projectKey + "&issue-key=" + issueKey + "&issue-id=" + issueId;
    var url = AJS.contextPath() + "/rest/octane-coverage/1.0/coverage?" + query;
    console.log("loadOctaneCoverageWidget :" + url);

    //do request
    $.ajax({url: url, type: "GET", dataType: "json", contentType: "application/json"
    }).done(function (data) {
        var panelEl = $("#octane-coverage-panel");
        var issueKeyFromHtml = panelEl.attr("issue-key");
        var issueKeyFromData = data.issueKey;
        if (issueKeyFromHtml !== issueKeyFromData) {
            console.log("issueKeyFromHtml(" + issueKeyFromHtml + ")!==issueKeyFromData(" + issueKeyFromData + ") => return");
            return;
        }

        $("#octane-loading-section").addClass("hidden");

        if (data.status === 'noData') {
            $("#octane-no-data-section").removeClass("hidden");
        } else if (data.status === 'noValidConfiguration') {
            $("#octane-no-valid-configuration-section").removeClass("hidden");
        } else if (data.status === 'hasData') {
            $("#octane-entity-section").removeClass("hidden");

            //entity settings
            var octaneEntity = data.octaneEntity.fields;
            $("#octane-entity-icon-text").text(octaneEntity.typeAbbreviation);
            $("#octane-entity-icon").css("background-color", octaneEntity.typeColor);
            $("#octane-entity-url a").attr("href", octaneEntity.url);
            $("#octane-entity-url a").text(octaneEntity.id);
            $("#octane-entity-name").text(octaneEntity.name);
            $("#octane-entity-name").attr("title", octaneEntity.name);

            //totals
            var totalRuns = data.totalExecutedTestsCount ? data.totalExecutedTestsCount + " last runs:" : "No last runs";
            $("#octane-total-runs").text(totalRuns);
            if (data.totalTestsCount) {
                $("#octane-total-tests").text(data.totalTestsCount);
                $("#view-tests-in-alm").attr("href", octaneEntity.testTabUrl);

            } else {
                $("#view-tests-in-alm").text("No linked tests in ALM Octane");
                $("#view-tests-in-alm").attr("style", "pointer-events: none; cursor: default; color: gray; font-weight: bold");
            }

            //coverage groups
            data.coverageGroups.forEach(function (entry) {
                var idSelector = "#" + entry.fields.id;
                var countSelector = idSelector + " .octane-test-status-count";
                var percentageSelector = idSelector + " .octane-test-status-percentage";
                $(idSelector).removeClass("hidden");
                $(countSelector).text(entry.fields.countStr);
                $(percentageSelector).text(entry.fields.percentage);
            });
        }

        if (data.debug) {
            $("#octane-debug-section").removeClass("hidden");
            $.each(data.debug, function (key, val) {
                $("#octane-debug-section").append("<p>" + key + " : " + val + "</p>");
            });
        }

    }).fail(function (request, status, error) {
        console.log(request.responseText);
    });
}