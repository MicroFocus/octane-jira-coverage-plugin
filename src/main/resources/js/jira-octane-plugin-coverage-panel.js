jQuery(document).ready(function() {
    load(1);
});

function load(counter) {
    const panelEl = jQuery("#octane-coverage-panel:not(.resolved)");

    if (!panelEl.length) {
        if(counter < 30) {
            setTimeout(function () {
                load(counter + 1);
            }, 150);
        }
    } else {
        console.log("[coverage] octane plugin load successfully " + counter);
        const projectKey = panelEl.attr("project-key");
        const issueKey = panelEl.attr("issue-key");
        const issueId = panelEl.attr("issue-id");
        const issueType = panelEl.attr("issue-type");

        jQuery("#octane-coverage-panel").addClass("resolved");

        loadOctaneWorkspaces(projectKey, issueKey, issueId, issueType);
    }
}

function loadOctaneWorkspaces(projectKey, issueKey, issueId, issueType) {
    return new Promise(function (resolve, reject) {
        const query = "project-key=" + projectKey + "&issue-type=" + issueType;
        const dataUrl = AJS.contextPath() + "/rest/octane-coverage/1.0/coverage/octane-workspaces?" + query;

        jQuery.ajax({url: dataUrl, type: "GET", dataType: "json", contentType: "application/json"})
            .done(function (data) {
                configureOctaneWorkspacesDropdown(data, projectKey, issueKey, issueId);
            })
            .fail(function (request) {
                const msg = !request.responseText ? request.statusText : request.responseText;
                reject(Error(msg));
            });
    });
}

function configureOctaneWorkspacesDropdown(data, projectKey, issueKey, issueId) {
    let dropdownWorkspaces = _.sortBy(data.flatMap(wsConfig => {
        return wsConfig.octaneWorkspaces.map(octaneWs => {
            return {
                "id": wsConfig.id + "-" + octaneWs.id,
                "wsConfigId": wsConfig.id,
                "workspaceId": octaneWs.id,
                "text": octaneWs.name + " (" + wsConfig.spaceConfigName + ")"
            }
        })
    }), 'text');

    AJS.$("#coverageWorkspaceSelector").auiSelect2({
        multiple: false,
        data: dropdownWorkspaces,
    });

    //ON-CHANGE
    jQuery("#coverageWorkspaceSelector").change(async function () {
        const selectedWorkspace = jQuery("#coverageWorkspaceSelector").auiSelect2('data');

        hideAllSectionsAndShowLoading();
        disableCoverageWorkspaceSelector();

        await loadOctaneCoverageWidget(projectKey, issueKey, issueId, selectedWorkspace.wsConfigId, selectedWorkspace.workspaceId)

        if (dropdownWorkspaces.length !== 1) {
            enableCoverageWorkspaceSelector();
        }
    });

    const selectedWorkspace = dropdownWorkspaces[0];
    jQuery("#coverageWorkspaceSelector").val(selectedWorkspace.id).trigger('change');
}

async function loadOctaneCoverageWidget(projectKey, issueKey, issueId, workspaceConfigId, workspaceId) {
    const query = "&project-key=" + projectKey + "&issue-key=" + issueKey + "&issue-id=" + issueId + "&workspace-config-id=" + workspaceConfigId + "&workspace-id=" + workspaceId;
    const url = AJS.contextPath() + "/rest/octane-coverage/1.0/coverage?" + query;
    console.log("[coverage] loadOctaneCoverageWidget :" + url);

    //do request
    await jQuery.ajax({
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

            clearOctaneRunGroups();

            //entity settings
            var octaneEntity = data.octaneEntity.fields;
            jQuery("#octane-entity-icon-text").text(octaneEntity.typeAbbreviation);
            jQuery("#octane-entity-icon").css("background-color", octaneEntity.typeColor);
            jQuery("#octane-entity-url a").attr("href", octaneEntity.url);
            jQuery("#octane-entity-url a").text(octaneEntity.id);
            jQuery("#octane-entity-name").text(octaneEntity.name);
            jQuery("#octane-entity-name").attr("title", octaneEntity.name);

            //totals
            let totalRuns;
            if (data.totalExecutedTestsCount) {
                totalRuns = data.totalExecutedTestsCount + " last runs:";
            } else {
                totalRuns = "No last runs";
            }
            jQuery("#octane-total-runs").text(totalRuns);

            if (data.totalTestsCount) {
                jQuery("#octane-total-tests").text(data.totalTestsCount);
                jQuery("#view-tests-in-alm").attr("href", octaneEntity.testTabUrl);
                showViewTestsInAlmSpan();
            } else {
                showNoLinkedTestsInAlmSpan();
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

function clearOctaneRunGroups() {
    const octaneRunsListEl = document.getElementById("octane-runs-list");
    for (const child of octaneRunsListEl.children) {
        var idSelector = "#" + child.id;
        jQuery(idSelector).addClass("hidden");
    }
}

function hideAllSectionsAndShowLoading() {
    jQuery("#octane-entity-section").addClass('hidden');
    jQuery("#octane-no-data-section").addClass('hidden');
    jQuery("#octane-no-valid-configuration-section").addClass('hidden');
    jQuery("#octane-loading-section").removeClass('hidden');
}

function disableCoverageWorkspaceSelector() {
    jQuery('#coverageWorkspaceSelector').prop('disabled', true);

    jQuery('#coverageWorkspaceSelector').addClass('pointer-events--none');
    jQuery('#coverageWorkspaceSelector').addClass('opacity--50');
}

function enableCoverageWorkspaceSelector() {
    jQuery('#coverageWorkspaceSelector').prop('disabled', false);

    jQuery('#coverageWorkspaceSelector').removeClass('pointer-events--none');
    jQuery('#coverageWorkspaceSelector').removeClass('opacity--50');
}

function showViewTestsInAlmSpan() {
    jQuery('#view-tests-in-alm').removeClass('hidden');
    jQuery('#no-linked-tests-in-alm').addClass('hidden');
}

function showNoLinkedTestsInAlmSpan() {
    jQuery('#view-tests-in-alm').addClass('hidden');
    jQuery('#no-linked-tests-in-alm').removeClass('hidden');
}