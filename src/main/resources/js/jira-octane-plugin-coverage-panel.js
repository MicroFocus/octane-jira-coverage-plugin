jQuery(document).ready(function() {
    load(1);
});

// Caching (TTL + LRU + per-workspace correctness) is delegated to OctaneCoveragePanelCache
// (see octane-coverage-panel-cache.js), which is loaded before this script. This is what
// lets us avoid re-fetching data from Octane on every issue save (Jira re-injects the
// right-context/board-detail-tab web-panels via AJAX after a save, without a full page
// reload), while still fetching once for a real initial load/navigation to an issue.

// Refresh button: delegated handler bound once per page, so it keeps working after Jira
// re-injects the panel HTML (e.g. after a save) without ever being bound twice.
jQuery(document).on("click", "#octaneDataReloadButton", function (e) {
    e.preventDefault();
    forceReloadOctaneCoverage();
});

function readPluginParameters(panelEl) {
    try {
        return JSON.parse(panelEl.attr("data-plugin-parameters") || "{}");
    } catch (e) {
        console.warn("[coverage] invalid plugin parameters on the panel, caching stays disabled");
        return {};
    }
}

function applyCacheSettingsFromPanel(panelEl) {
    OctaneCoveragePanelCache.configure(readPluginParameters(panelEl)); // reads only its own parameters
}

function readPanelContext(panelEl) {
    return {
        panelElement: panelEl[0],
        projectKey: panelEl.attr("project-key"),
        issueKey: panelEl.attr("issue-key"),
        issueId: panelEl.attr("issue-id"),
        issueType: panelEl.attr("issue-type")
    };
}

function isPanelShowingIssue(issueId) {
    return jQuery("#octane-coverage-panel").attr("issue-id") === String(issueId);
}

function isContextCurrent(ctx) {
    return ctx.panelElement.isConnected && isPanelShowingIssue(ctx.issueId);
}

function load(counter) {
    const panelEl = jQuery("#octane-coverage-panel:not(.resolved)");

    if (!panelEl.length) {
        if(counter < 30) {
            setTimeout(function () {
                load(counter + 1);
            }, 150);
        }
    } else {
        const ctx = readPanelContext(panelEl);
        panelEl.addClass("resolved");

        applyCacheSettingsFromPanel(panelEl);

        // Drops cached data if it was fetched for another project / issue type / key
        // (e.g. issue type edited inline and saved -> panel re-rendered with new attributes).
        OctaneCoveragePanelCache.ensureContext(ctx.issueId, ctx.projectKey, ctx.issueType, ctx.issueKey);

        const cachedWorkspacesData = OctaneCoveragePanelCache.getWorkspacesData(ctx.issueId);
        if (cachedWorkspacesData) {
            configureOctaneWorkspacesDropdown(cachedWorkspacesData, ctx);
        } else {
            loadOctaneWorkspaces(ctx);
        }
    }
}

function forceReloadOctaneCoverage() {
    const panelEl = jQuery("#octane-coverage-panel");
    if (!panelEl.length || isReloadInProgress()) {
        return;
    }

    const ctx = readPanelContext(panelEl);

    OctaneCoveragePanelCache.invalidateIssue(ctx.issueId);
    OctaneCoveragePanelCache.ensureContext(ctx.issueId, ctx.projectKey, ctx.issueType, ctx.issueKey);

    hideAllSectionsAndShowLoading();
    startReloadSpinner();
    loadOctaneWorkspaces(ctx);
}

function loadOctaneWorkspaces(ctx) {
    const generation = OctaneCoveragePanelCache.getGeneration(ctx.issueId);
    const query = "project-key=" + encodeURIComponent(ctx.projectKey) + "&issue-type=" + encodeURIComponent(ctx.issueType);
    const dataUrl = AJS.contextPath() + "/rest/octane-coverage/1.0/coverage/octane-workspaces?" + query;

    // De-duplicated: a panel re-rendered while this request is still running joins it instead of re-sending it.
    OctaneCoveragePanelCache.dedupeInFlight(ctx.issueId, "workspaces", function () {
        return jQuery.ajax({url: dataUrl, type: "GET", dataType: "json", contentType: "application/json"});
    }).then(function (data) {
        // Not stored if the issue was invalidated (manual refresh / context change) meanwhile.
        OctaneCoveragePanelCache.setWorkspacesData(ctx.issueId, data, generation);

        if (!OctaneCoveragePanelCache.isCurrentGeneration(ctx.issueId, generation) || !isContextCurrent(ctx)) {
            return;
        }
        configureOctaneWorkspacesDropdown(data, ctx);
    }, function (request) {
        if (!OctaneCoveragePanelCache.isCurrentGeneration(ctx.issueId, generation) || !isContextCurrent(ctx)) {
            return;
        }
        console.warn("[coverage] failed to load workspaces: " + (request && (request.responseText || request.statusText)));

        showErrorSection();
        stopReloadSpinner();
    });
}

function configureOctaneWorkspacesDropdown(data, ctx) {
    const dropdownWorkspaces = _.sortBy(data.flatMap(wsConfig => {
        return wsConfig.octaneWorkspaces.map(octaneWs => {
            return {
                "id": wsConfig.id + "-" + octaneWs.id,
                "wsConfigId": wsConfig.id,
                "workspaceId": octaneWs.id,
                "text": octaneWs.name + " (" + wsConfig.spaceConfigName + ")"
            }
        })
    }), 'text');

    const selectorEl = jQuery("#coverageWorkspaceSelector");
    selectorEl.auiSelect2({
        multiple: false,
        data: dropdownWorkspaces,
    });

    // Handlers are namespaced and unbound first: this function runs again on the same element
    // after a manual refresh, and must not stack duplicate handlers (each one = 1 more request).
    selectorEl.off("change.octaneCoverage").on("change.octaneCoverage", function () {
        selectWorkspace(ctx, selectorEl.auiSelect2('data'), dropdownWorkspaces.length);
    });

    // hacky solution to show tooltip on select2 items (used because there is no API on select2 or auiselect2 for this)
    selectorEl.off("select2-open.octaneCoverage").on("select2-open.octaneCoverage", function () {
        Array.from(jQuery("#select2-drop")[0].children[1].children).forEach(function (item) {
            item.title = item.firstChild.lastChild.textContent;
        });
    });

    const lastSelectedKey = OctaneCoveragePanelCache.getSelectedWorkspaceKey(ctx.issueId);
    const initialWorkspace = dropdownWorkspaces.find(ws => ws.id === lastSelectedKey) || dropdownWorkspaces[0];

    if (!initialWorkspace) {
        hideAllSections();
        jQuery("#octane-no-valid-configuration-section").removeClass("hidden");
        stopReloadSpinner();
        return;
    }

    selectorEl.auiSelect2("val", initialWorkspace.id);
    selectWorkspace(ctx, initialWorkspace, dropdownWorkspaces.length);
}

async function selectWorkspace(ctx, workspace, workspacesCount) {
    if (!workspace) {
        return;
    }

    const workspaceKey = OctaneCoveragePanelCache.workspaceKeyOf(workspace.wsConfigId, workspace.workspaceId);
    OctaneCoveragePanelCache.setSelectedWorkspaceKey(ctx.issueId, workspaceKey);
    modifyCoverageWorkspaceSelectorTooltip(workspace.text);

    const cachedCoverageData = OctaneCoveragePanelCache.getCoverage(ctx.issueId, workspaceKey);
    if (cachedCoverageData) {
        renderCoverageData(cachedCoverageData, ctx.issueId, workspaceKey);
        setCoverageWorkspaceSelectorEnabled(workspacesCount !== 1);
        stopReloadSpinner();
        return;
    }

    hideAllSectionsAndShowLoading();
    setCoverageWorkspaceSelectorEnabled(false);

    try {
        await loadOctaneCoverageWidget(ctx, workspace.wsConfigId, workspace.workspaceId);
    } finally {
        // only the request of the currently selected workspace (of the panel still in the page)
        // controls the dropdown state
        if (isContextCurrent(ctx) && OctaneCoveragePanelCache.getSelectedWorkspaceKey(ctx.issueId) === workspaceKey) {
            setCoverageWorkspaceSelectorEnabled(workspacesCount !== 1);
        }
    }
}

// Always resolves (never rejects): failures are rendered as an error state in the panel.
function loadOctaneCoverageWidget(ctx, workspaceConfigId, workspaceId) {
    const query = "project-key=" + encodeURIComponent(ctx.projectKey)
        + "&issue-key=" + encodeURIComponent(ctx.issueKey)
        + "&issue-id=" + encodeURIComponent(ctx.issueId)
        + "&workspace-config-id=" + encodeURIComponent(workspaceConfigId)
        + "&workspace-id=" + encodeURIComponent(workspaceId);
    const url = AJS.contextPath() + "/rest/octane-coverage/1.0/coverage?" + query;

    const workspaceKey = OctaneCoveragePanelCache.workspaceKeyOf(workspaceConfigId, workspaceId);
    const generation = OctaneCoveragePanelCache.getGeneration(ctx.issueId);

    // De-duplicated per issue + workspace: e.g. a panel re-rendered by a save while this request is
    // running, or a quick A -> B -> A workspace switch, joins the running request instead of re-sending it.
    return OctaneCoveragePanelCache.dedupeInFlight(ctx.issueId, "coverage:" + workspaceKey, function () {
        return jQuery.ajax({url: url, type: "GET", dataType: "json", contentType: "application/json"});
    }).then(function (data) {
        OctaneCoveragePanelCache.setCoverage(ctx.issueId, workspaceKey, data, generation);

        if (OctaneCoveragePanelCache.isCurrentGeneration(ctx.issueId, generation) && isContextCurrent(ctx)) {
            if (renderCoverageData(data, ctx.issueId, workspaceKey)) {
                stopReloadSpinner();
            }
        }
    }, function (request) {
        if (OctaneCoveragePanelCache.isCurrentGeneration(ctx.issueId, generation) && isContextCurrent(ctx)
            && isSelectedAndShown(ctx.issueId, workspaceKey)) {
            console.warn("[coverage] failed to load coverage: " + (request && (request.responseText || request.statusText)));
            showErrorSection();
            stopReloadSpinner();
        }
    });
}

function isSelectedAndShown(issueId, workspaceKey) {
    return isPanelShowingIssue(issueId)
        && OctaneCoveragePanelCache.getSelectedWorkspaceKey(issueId) === workspaceKey;
}

// Returns true if the data was rendered, false if it was ignored because it no longer matches
// what the panel shows (another issue, or the user already switched to another workspace).
function renderCoverageData(data, issueId, workspaceKey) {
    if (!isSelectedAndShown(issueId, workspaceKey)) {
        return false;
    }

    const issueKeyFromHtml = jQuery("#octane-coverage-panel").attr("issue-key");
    if (issueKeyFromHtml !== data.issueKey) {
        console.warn("[coverage] issueKeyFromHtml(" + issueKeyFromHtml + ")!==issueKeyFromData(" + data.issueKey + ") => ignored");
        return false;
    }

    // reset previously rendered sections before applying the (possibly cached) data
    hideAllSections();

    if (data.status === 'noData') {
        jQuery("#octane-no-data-section").removeClass("hidden");
    } else if (data.status === 'noValidConfiguration') {
        jQuery("#octane-no-valid-configuration-section").removeClass("hidden");
    } else if (data.status === 'exceedsMaxTotalCount') {
        jQuery("#octane-exceeds-max-total-count-section").removeClass("hidden");
    } else if (data.status === 'hasData') {
        jQuery("#octane-entity-section").removeClass("hidden");

        clearOctaneRunGroups();

        //entity settings
        const octaneEntity = data.octaneEntity.fields;
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
            const idSelector = "#" + entry.fields.id;
            const countSelector = idSelector + " .octane-test-status-count";
            const percentageSelector = idSelector + " .octane-test-status-percentage";
            jQuery(idSelector).removeClass("hidden");
            jQuery(countSelector).text(entry.fields.countStr);
            jQuery(percentageSelector).text(entry.fields.percentage);
        });
    }

    // Emptied first: the panel DOM is reused on manual refresh / workspace switch, so appending
    // without clearing would duplicate the lines. Rendered as text, never as HTML.
    const debugSectionEl = jQuery("#octane-debug-section").empty().addClass("hidden");
    if (data.debug) {
        debugSectionEl.removeClass("hidden");
        jQuery.each(data.debug, function (key, val) {
            debugSectionEl.append(jQuery("<p>").text(key + " : " + val));
        });
    }

    return true;
}

function clearOctaneRunGroups() {
    const octaneRunsListEl = document.getElementById("octane-runs-list");
    for (const child of octaneRunsListEl.children) {
        jQuery("#" + child.id).addClass("hidden");
    }
}

function hideAllSections() {
    jQuery("#octane-loading-section").addClass('hidden');
    jQuery("#octane-entity-section").addClass('hidden');
    jQuery("#octane-no-data-section").addClass('hidden');
    jQuery("#octane-no-valid-configuration-section").addClass('hidden');
    jQuery("#octane-exceeds-max-total-count-section").addClass('hidden');
    jQuery("#octane-error-section").addClass('hidden');
}

function hideAllSectionsAndShowLoading() {
    hideAllSections();
    jQuery("#octane-loading-section").removeClass('hidden');
}

function showErrorSection() {
    hideAllSections();
    jQuery("#octane-error-section").removeClass('hidden');
}

function setCoverageWorkspaceSelectorEnabled(enabled) {
    const selectorEl = jQuery("#coverageWorkspaceSelector");
    selectorEl.prop("disabled", !enabled);
    selectorEl.toggleClass("pointer-events--none", !enabled);
    selectorEl.toggleClass("opacity--50", !enabled);
}

function modifyCoverageWorkspaceSelectorTooltip(tooltipText) {
    jQuery("#s2id_coverageWorkspaceSelector").attr("title",  tooltipText);
    jQuery("#octane-workspaces-dropdown-section").attr("title",  tooltipText);
}

function showViewTestsInAlmSpan() {
    jQuery("#view-tests-in-alm").removeClass("hidden");
    jQuery("#no-linked-tests-in-alm").addClass("hidden");
}

function showNoLinkedTestsInAlmSpan() {
    jQuery("#view-tests-in-alm").addClass("hidden");
    jQuery("#no-linked-tests-in-alm").removeClass("hidden");
}

function isReloadInProgress() {
    return jQuery("#octaneDataReloadButton").attr("aria-busy") === "true";
}

function startReloadSpinner() {
    jQuery("#octaneDataReloadButton").attr("aria-busy", "true").prop("disabled", true)
        .find(".aui-icon").addClass("spin");
}

function stopReloadSpinner() {
    jQuery("#octaneDataReloadButton").attr("aria-busy", "false").prop("disabled", false)
        .find(".aui-icon").removeClass("spin");
}
