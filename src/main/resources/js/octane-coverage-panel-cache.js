/**
 * OctaneCoveragePanelCache
 * -------------------------
 * Dedicated, self-contained caching layer for the Octane test-coverage web-panel.
 *
 * WHY THIS EXISTS:
 * Jira re-injects the coverage web-panel's HTML fragment (right-context panel and
 * board "details" tab) not only on a genuine issue load/navigation, but also after
 * an issue is saved (Jira does a partial AJAX refresh of the issue view, without a
 * full page reload). Without this cache, every save would trigger a brand new
 * fetch to Octane, which does not scale for large user bases (thousands of
 * concurrent saves/loads flooding Octane).
 *
 * This module lets the panel script redraw itself from the last known-good Octane
 * response after a save, instead of re-fetching, while still fetching once for a
 * real initial load/navigation to a (previously unseen, or expired) issue.
 *
 * LIFETIME / SCOPE:
 * This object lives at module scope, so it survives the panel being torn down and
 * re-rendered by Jira after a save. It is entirely reset when the browser does a
 * real full page navigation/reload (the whole JS context, including this module,
 * gets torn down and reloaded), so there is no cross-session persistence.
 *
 * EVICTION POLICY (kept intentionally simple):
 *  - Disabled: an admin can set the cache duration to 0 (e.g. small installations that prefer
 *    always up-to-date data). Nothing is then stored and every read is a miss, i.e. the panel
 *    behaves as before this cache existed. Only in-flight de-duplication stays active (it never
 *    returns old data, it just avoids sending the exact same request twice at the same time).
 *  - TTL (time-to-live), checked lazily on read: every cached entry is stamped
 *    with the time it was fetched. When something is read, if it is older than
 *    TTL_MS it is deleted and treated as a cache miss (triggering a real re-fetch).
 *    There is no background timer/sweep - expiry is only ever evaluated at read time.
 *  - LRU (least-recently-used) cap, checked on write: at most MAX_ISSUES issues are
 *    tracked at any time. When adding a new issue would exceed that cap, the
 *    least-recently-touched issue is evicted first.
 *  - Manual invalidation: invalidateIssue(issueId) drops all cached data for one
 *    issue (used by the manual "refresh" button), so the very next load is a real
 *    fetch from Octane. The last selected workspace is kept.
 *  - Context invalidation: ensureContext(...) drops all cached data for an issue if
 *    its project / issue type / key differ from the ones the data was fetched for.
 *  - Stale responses: every write takes the "generation" captured before the fetch;
 *    responses of requests started before an invalidation are not stored.
 */
var OctaneCoveragePanelCache = (function () {
    "use strict";

    var TTL_PARAMETER = "TEST_COVERAGE_CACHE_DURATION_IN_SECONDS";

    var TTL_MS = 0;       // 0 = caching disabled (admin can set the duration to 0 on purpose)
    var MAX_ISSUES = 50;  // developer-controlled maximum number of distinct issues tracked at once

    function configure(parameters) {
        if (!parameters) {
            return;
        }
        var ttlSeconds = parameters[TTL_PARAMETER];
        if (Number.isInteger(ttlSeconds) && ttlSeconds >= 0) {
            TTL_MS = ttlSeconds * 1000;
        }
    }

    function isCachingEnabled() {
        return TTL_MS > 0;
    }

    // issueId (string) -> {
    //   lastAccess: number,
    //   generation: number,       // changes on every invalidation, see getGeneration()
    //   context: string,          // projectKey|issueType|issueKey the data was fetched for
    //   workspacesData: { value: any, fetchedAt: number } | undefined,
    //   selectedWorkspaceKey: string | undefined,
    //   coverageByWorkspace: { [workspaceKey]: { value: any, fetchedAt: number } },
    //   inFlight: { [workspaceKey]: Promise } // requests currently in progress, for de-dupe
    // }
    var issues = {};

    // Module-wide, monotonically increasing: a generation value is never reused, not even if
    // an issue entry is evicted (LRU) and later re-created while an old request is in flight.
    var nextGeneration = 1;

    function now() {
        return Date.now();
    }

    function isExpired(entry) {
        return !entry || !isCachingEnabled() || (now() - entry.fetchedAt) > TTL_MS;
    }

    function getOrCreateIssueEntry(issueId) {
        var entry = issues[issueId];

        if (!entry) {
            entry = {
                lastAccess: now(),
                generation: nextGeneration++,
                context: undefined,
                workspacesData: undefined,
                selectedWorkspaceKey: undefined,
                coverageByWorkspace: {},
                inFlight: {}
            };
            issues[issueId] = entry;
            evictIfNeeded();
        }
        entry.lastAccess = now();
        return entry;
    }

    function clearData(entry) {
        entry.generation = nextGeneration++;
        entry.workspacesData = undefined;
        entry.coverageByWorkspace = {};
        entry.inFlight = {};
    }

    /**
     * Makes sure the cached data for issueId was fetched for the same project / issue type /
     * issue key as the one currently rendered. The list of Octane workspaces is filtered by
     * project + issue type, so if any of those changed (e.g. the issue type was edited inline
     * and saved, which re-renders the panel without a page reload), all cached data for the
     * issue is dropped and the next read is a cache miss. The workspace selection is kept.
     */
    function ensureContext(issueId, projectKey, issueType, issueKey) {
        var context = [projectKey, issueType, issueKey].join("|");
        var entry = getOrCreateIssueEntry(issueId);

        if (entry.context !== context) {
            if (entry.context !== undefined) {
                clearData(entry);
            }
            entry.context = context;
        }
    }

    /**
     * Returns a token identifying the current "version" of the cached data of an issue.
     * Capture it right before a fetch and pass it back to setWorkspacesData/setCoverage:
     * if the issue was invalidated in the meantime (manual refresh, context change, eviction),
     * the late response is not stored, so it cannot overwrite / resurrect fresher data.
     */
    function getGeneration(issueId) {
        return getOrCreateIssueEntry(issueId).generation;
    }

    function isCurrentGeneration(issueId, generation) {
        var entry = issues[issueId];
        return !!entry && entry.generation === generation;
    }

    function evictIfNeeded() {
        var issueIds = Object.keys(issues);
        if (issueIds.length <= MAX_ISSUES) {
            return;
        }

        // Evict the least-recently-touched issue(s) until back within the cap.
        issueIds.sort(function (a, b) {
            return issues[a].lastAccess - issues[b].lastAccess;
        });

        var numberToEvict = issueIds.length - MAX_ISSUES;
        for (var i = 0; i < numberToEvict; i++) {
            delete issues[issueIds[i]];
        }
    }

    function workspaceKeyOf(workspaceConfigId, workspaceId) {
        return workspaceConfigId + "-" + workspaceId;
    }

    function getWorkspacesData(issueId) {
        var entry = issues[issueId];
        if (!entry || !entry.workspacesData) {
            return undefined;
        }
        if (isExpired(entry.workspacesData)) {
            entry.workspacesData = undefined;
            return undefined;
        }
        entry.lastAccess = now();
        return entry.workspacesData.value;
    }

    /** Stores data only if caching is enabled and 'generation' (see getGeneration) is still current; returns whether it was stored. */
    function setWorkspacesData(issueId, data, generation) {
        if (!isCachingEnabled() || !isCurrentGeneration(issueId, generation)) {
            return false;
        }
        var entry = getOrCreateIssueEntry(issueId);
        entry.workspacesData = {value: data, fetchedAt: now()};
        return true;
    }

    function getCoverage(issueId, workspaceKey) {
        var entry = issues[issueId];
        if (!entry) {
            return undefined;
        }
        var covEntry = entry.coverageByWorkspace[workspaceKey];
        if (!covEntry) {
            return undefined;
        }
        if (isExpired(covEntry)) {
            delete entry.coverageByWorkspace[workspaceKey];
            return undefined;
        }
        entry.lastAccess = now();
        return covEntry.value;
    }

    /** Stores data only if caching is enabled and 'generation' (see getGeneration) is still current; returns whether it was stored. */
    function setCoverage(issueId, workspaceKey, data, generation) {
        if (!isCachingEnabled() || !isCurrentGeneration(issueId, generation)) {
            return false;
        }
        var entry = getOrCreateIssueEntry(issueId);
        entry.coverageByWorkspace[workspaceKey] = {value: data, fetchedAt: now()};
        return true;
    }

    function getSelectedWorkspaceKey(issueId) {
        var entry = issues[issueId];
        return entry ? entry.selectedWorkspaceKey : undefined;
    }

    function setSelectedWorkspaceKey(issueId, workspaceKey) {
        var entry = getOrCreateIssueEntry(issueId);
        entry.selectedWorkspaceKey = workspaceKey;
    }

    /**
     * Drops all cached Octane data for the issue (used by the manual refresh button), so the next
     * load is a real fetch. The last selected workspace is intentionally kept, so a refresh
     * reloads the workspace the user is looking at instead of jumping back to the first one.
     * Any request already in flight for this issue becomes stale (its result won't be stored).
     */
    function invalidateIssue(issueId) {
        var entry = issues[issueId];
        if (entry) {
            clearData(entry);
        }
    }

    /**
     * Runs fetchFn() for (issueId, requestKey), unless a request for the exact same key is already
     * in progress, in which case the existing in-flight promise is returned instead of firing a
     * duplicate request to Octane. Typical case: the user saves the issue while the initial fetch is
     * still running -> Jira re-renders the panel, which finds nothing cached yet and would otherwise
     * send the very same request again.
     *
     * fetchFn may return any thenable (e.g. a jqXHR); it is normalized to a native Promise. The
     * caller remains responsible for storing the result (setWorkspacesData / setCoverage).
     * invalidateIssue() / a context change drops the in-flight bookkeeping, so a manual refresh
     * always sends a new request instead of joining an older one.
     */
    function dedupeInFlight(issueId, requestKey, fetchFn) {
        var entry = getOrCreateIssueEntry(issueId);
        var inFlight = entry.inFlight;
        var existing = inFlight[requestKey];
        if (existing) {
            return existing;
        }

        var promise = Promise.resolve(fetchFn());
        inFlight[requestKey] = promise;

        var cleanup = function () {
            // Only remove our own entry: after an invalidation 'inFlight' may already hold a newer request.
            if (inFlight[requestKey] === promise) {
                delete inFlight[requestKey];
            }
        };
        promise.then(cleanup, cleanup);
        return promise;
    }

    return {
        configure: configure,
        ensureContext: ensureContext,
        getGeneration: getGeneration,
        isCurrentGeneration: isCurrentGeneration,
        workspaceKeyOf: workspaceKeyOf,
        getWorkspacesData: getWorkspacesData,
        setWorkspacesData: setWorkspacesData,
        getCoverage: getCoverage,
        setCoverage: setCoverage,
        getSelectedWorkspaceKey: getSelectedWorkspaceKey,
        setSelectedWorkspaceKey: setSelectedWorkspaceKey,
        invalidateIssue: invalidateIssue,
        dedupeInFlight: dedupeInFlight
    };
})();
