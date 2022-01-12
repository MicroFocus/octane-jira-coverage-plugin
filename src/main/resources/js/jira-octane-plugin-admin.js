(function ($) { // this closure helps us keep our variables to ourselves.
    // This pattern is known as an "iife" - immediately invoked function expression
    var octanePluginContext = {};
    var iconWaitClass = "aui-icon-wait icon throbber loading";//jira7 : aui-icon-wait; jira8: icon throbber loading
    var iconOkClass = "aui-iconfont-successful-build";
    var iconFailedClass = "aui-iconfont-error";

    octanePluginContext.octaneAdminBaseUrl = AJS.contextPath() + "/rest/octane-admin/1.0/";


    // wait for the DOM (i.e., document "skeleton") to load. This likely isn't necessary for the current case,
    // but may be helpful for AJAX that provides secondary content.
    $(document).ready(function () {
        window.onbeforeunload = null;//Disable “Changes you made may not be saved” pop-up window
        configureSpaceDialog();
        configureSpaceTable();

        configureWorkspaceDialog();
        configureWorkspaceTable();
        configureProxyDialog();
    });

    function configureSpaceTable() {

        var MyRow = AJS.RestfulTable.Row.extend({
            renderOperations: function () {
                var instance = this;

                var editButtonEl = $('<button class=\"aui-button aui-button-link\">Edit</button>').click(function (e) {
                    showSpaceDialog(instance);
                });

                var deleteButtonEl = $('<button class=\"aui-button aui-button-link\">Delete</button>').click(function (e) {
                    var msg = "Are you sure you want to delete '" + instance.model.attributes.name + "' space configuration?";
                    removeRow(msg, octanePluginContext.spaceTable, instance);
                });

                var testConnectionButtonEl = $('<button class=\"aui-button aui-button-link\">Test Connection</button>').click(function (e) {
                    testConnection(instance);
                });

                var parentEl = $('<div></div>').append(editButtonEl, deleteButtonEl, testConnectionButtonEl);
                return parentEl;
            }
        });

        octanePluginContext.spacesUrl = octanePluginContext.octaneAdminBaseUrl + "spaces";
        octanePluginContext.spaceTable = new AJS.RestfulTable({
            el: jQuery("#space-table"),
            resources: {
                all: octanePluginContext.octaneAdminBaseUrl + "spaces"
            },
            columns: [
                {id: "name", header: "Name"},
                {id: "location", header: "Location"},
                {id: "clientId", header: "Client ID"}

            ],
            autoFocus: false,
            allowEdit: false,
            allowReorder: false,
            allowCreate: false,
            allowDelete: false,
            noEntriesMsg: "No space configuration is defined.",
            loadingMsg: "Loading ...",
            views: {
                row: MyRow
            }
        });
    }

    function configureWorkspaceTable() {
        var ListReadView = AJS.RestfulTable.CustomReadView.extend({
            render: function (self) {
                var output = _.reduce(self.value, function (memo, current) {
                    return memo + '<li>' + current + '</li>';
                }, '<ul class="simple-list">');
                output += '</ul>';
                return output;
            }
        });

        var MyRow = AJS.RestfulTable.Row.extend({
            renderOperations: function () {
                var instance = this;

                var editButtonEl = $('<button class=\"aui-button aui-button-link\">Edit</button>').click(function (e) {
                    showWorkspaceDialog(instance);
                });

                var deleteButtonEl = $('<button class=\"aui-button aui-button-link\">Delete</button>').click(function (e) {
                    var msg = "Are you sure you want to delete '" + instance.model.attributes.workspaceName + "' workspace configuration?";
                    removeRow(msg, octanePluginContext.workspaceTable, instance);
                });

                var parentEl = $('<div></div>').append(editButtonEl, deleteButtonEl);
                return parentEl;
            }
        });

        octanePluginContext.workspaceTable = new AJS.RestfulTable({
            el: jQuery("#workspace-table"),
            resources: {
                all: octanePluginContext.octaneAdminBaseUrl + "workspaces"
            },
            columns: [
                {id: "spaceConfigName", header: "Space Name"},
                {id: "workspaceId", header: "Workspace Id"},
                {id: "workspaceName", header: "Workspace Name"},
                {id: "octaneUdf", header: "Mapping Field"},
                {id: "octaneEntityTypes", header: "Entity Types", readView: ListReadView},
                {id: "jiraIssueTypes", header: "Jira Issue Types", readView: ListReadView},
                {id: "jiraProjects", header: "Jira Project", readView: ListReadView}
            ],
            autoFocus: false,
            allowEdit: false,
            allowReorder: false,
            allowCreate: false,
            allowDelete: false,
            noEntriesMsg: "No workspace configuration is defined.",
            loadingMsg: "Loading ...",
            views: {
                row: MyRow
            }
        });
    }

    function removeRow(msg, table, row) {
        $("#warning-message").text(msg);

        AJS.dialog2("#warning-dialog").show();

        $("#warning-dialog-confirm").off();
        $("#warning-dialog-cancel").off();

        AJS.$("#warning-dialog-confirm").click(function (e) {
            e.preventDefault();
            AJS.dialog2("#warning-dialog").hide();

            $.ajax({
                url: table.options.resources.all + "/" + row.model.id, type: "DELETE",
            }).done(function () {
                table.removeRow(row);
                if (table === octanePluginContext.spaceTable) {
                    reloadTable(octanePluginContext.workspaceTable);
                }
            });
        });

        AJS.$("#warning-dialog-cancel").click(function (e) {
            e.preventDefault();
            AJS.dialog2("#warning-dialog").hide();
        });
    }

    function configureProxyDialog() {
        AJS.$("#show-proxy-settings").click(function (e) {
            e.preventDefault();
            $("#proxy-dialog .error").text('');//clear previous error messages
            $.ajax({
                url: octanePluginContext.octaneAdminBaseUrl + "proxy",
                type: "GET",
                dataType: "json",
                contentType: "application/json"
            }).done(function (data) {
                $("#proxyHost").val(data.host);
                $("#proxyPort").val(data.port);
                $("#proxyUser").val(data.username);
                $("#proxyPassword").val(data.password);
                $("#nonProxyHost").val(data.nonProxyHost);

                AJS.dialog2("#proxy-dialog").show();
            });
        });

        AJS.$("#proxy-submit-button").click(function (e) {
            e.preventDefault();
            var data = {
                host: $("#proxyHost").attr("value"),
                port: $("#proxyPort").attr("value"),
                username: $("#proxyUser").attr("value"),
                password: $("#proxyPassword").attr("value"),
                nonProxyHost: $("#nonProxyHost").attr("value"),
            };

            //validation
            if (data.host) {
                var validationFailed = false;
                validationFailed = !validateConditionAndUpdateErrorField(data.host.toLowerCase().indexOf("http") !== 0, "Enter host name without http://", "#proxyHostError") ||
                    !validateConditionAndUpdateErrorField(!(/:\d+/.test(data.host)), "Enter host name without port", "#proxyHostError") || validationFailed;
                validationFailed = !validateConditionAndUpdateErrorField((data.port && !isNaN(data.port)), "Port value must be a number.", "#proxyPortError") ||
                    !validateConditionAndUpdateErrorField((data.port >= 0 && data.port <= 65535), "Port must range from 0 to 65,535.", "#proxyPortError") ||
                    validationFailed;

                if (validationFailed) {
                    return;
                }
            }

            var myJSON = JSON.stringify(data);
            $.ajax({
                url: octanePluginContext.octaneAdminBaseUrl + "proxy",
                type: "PUT",
                data: myJSON,
                dataType: "json",
                contentType: "application/json"
            }).done(function (msg) {
                AJS.dialog2("#proxy-dialog").hide();
                setProxyStatus("Proxy settings are saved successfully", true);
            }).fail(function (request, status, error) {
                setProxyStatus(request.responseText, false);
            });
        });

        AJS.$("#proxy-cancel-button").click(function (e) {
            e.preventDefault();
            AJS.dialog2("#proxy-dialog").hide();
        });
    }

    function reloadPossibleJiraFields() {
        function setTitle(text, filled) {
            $("#octane-possible-fields-tooltip").attr("title", text);
            AJS.$("#octane-possible-fields-tooltip").tooltip();
            $("#octane-possible-fields-tooltip").toggleClass("aui-iconfont-info-filled", filled);
        }

        var workspaceId = $("#workspaceSelector").val();
        var spaceConfId = octanePluginContext.workspaceDialogData.spaceConf.id;
        octanePluginContext.workspaceDialogData.possibleJiraField = null;
        if (workspaceId) {
            setTitle("Searching...", false);
            $.ajax({
                url: octanePluginContext.octaneAdminBaseUrl + "workspaces/possible-jira-fields?space-config-id=" + spaceConfId + "&workspace-id=" + workspaceId,
                type: "GET",
                dataType: "json",
                contentType: "application/json"
            }).done(function (data) {
                if (data && data.length) {
                    setTitle("Suggested ALM Octane fields: " + data.join(",") + ". Double-click to set '" + data[0] + "' as value.", true);
                    octanePluginContext.workspaceDialogData.possibleJiraField = data[0];
                } else {
                    setTitle("No suggested fields are found.");
                }
            }).fail(function (request, status, error) {
                var msg = request.responseText;
                if (request.responseText && request.responseText.includes('platform.workspace_not_found')) {
                    msg = "Workspace " + workspaceId + "  is not accessible.";
                    AJS.flag({type: 'error', close: 'auto', body: msg});
                } else if (request.responseText && request.responseText.includes('platform.not_authorized')) {
                    msg = "The client ID configured for workspace " + workspaceId + " does not have permission to access it.";
                }
                setTitle("Failed to fetch suggested fields from ALM Octane: " + msg, true);
            });
        } else {
            setTitle("Select a workspace to show the list of suggested mapping fields that include 'jira' in their name.", false);
        }
    }

    function validateSpaceRequiredFieldsFilled() {
        if (!octanePluginContext.spaceSaveClicked) {
            return true;
        }

        //validate
        var nameValue = $("#name").attr("value").trim();
        var locationValue = $("#location").attr("value").trim();
        var clientIdValue = $("#clientId").attr("value").trim();

        var validationFailed = !validateMissingRequiredField(nameValue, "#nameError") ||
            !validateConditionAndUpdateErrorField((nameValue.length <= 40), "Exceeds allowed length (40 characters)", "#nameError");
        validationFailed = !validateMissingRequiredField(locationValue, "#locationError") || validationFailed;
        validationFailed = !validateMissingRequiredField(clientIdValue, "#clientIdError") || validationFailed;
        validationFailed = !validateMissingRequiredField($("#clientSecret").attr("value"), "#clientSecretError") || validationFailed;

        return !validationFailed;
    }

    function testConnectionFromDialog() {
        octanePluginContext.spaceSaveClicked = true;
        if (!validateSpaceRequiredFieldsFilled()) {
            return;
        }

        //build model
        var modelForUpdate = {
            name: $("#name").attr("value").trim(),
            location: $("#location").attr("value").trim(),
            clientId: $("#clientId").attr("value").trim(),
            clientSecret: $("#clientSecret").attr("value")
        };

        var isEditMode = !!octanePluginContext.spaceCurrentRow;
        if (isEditMode) {
            var rowModel = octanePluginContext.spaceCurrentRow.model.attributes;
            modelForUpdate.id = rowModel.id;
        }

        //send
        var myJSON = JSON.stringify(modelForUpdate);
        statusIconStart("#space-save-status", "Testing connection ...");

        $.ajax({
            url: octanePluginContext.spaceTable.options.resources.all + "/test-connection",
            type: "POST",
            data: myJSON,
            dataType: "json",
            contentType: "application/json"
        }).done(function (result) {
            statusIconOk("#space-save-status", "Test connection is successful");
        }).fail(function (response) {
            var msg = getFailedMessage(response);
            statusIconFailed("#space-save-status", msg);
        });
    }

    function testConnection(row) {
        var rowModel = row.model.attributes;
        var throbberStatusClassName = "throbber_status_" + rowModel.id;
        var throbberStatusClassNameSelector = "." + throbberStatusClassName;
        var throbber = $(throbberStatusClassNameSelector);
        if (!throbber.length) {
            var statusEl = row.$el.children().eq(4);

            if (!statusEl.children().length) {//for jira 8
                statusEl.append('<span></span>');
            }

            throbber = statusEl.children().first();
            throbber.addClass("throbber-status");
            throbber.addClass("aui-icon");
            throbber.addClass("aui-icon-small");
            throbber.removeClass("aui-restfultable-throbber");//for jira 7

            throbber.addClass(throbberStatusClassName);
        }

        statusIconStart(throbberStatusClassNameSelector, "Testing connection ...");

        //build model
        var modelForUpdate = {
            id: rowModel.id,
            name: rowModel.name,
            location: rowModel.location,
            clientId: rowModel.clientId,
            clientSecret: rowModel.clientSecret
        };

        //send
        var myJSON = JSON.stringify(modelForUpdate);
        $.ajax({
            url: octanePluginContext.spaceTable.options.resources.all + "/test-connection",
            type: "POST",
            data: myJSON,
            dataType: "json",
            contentType: "application/json"
        }).done(function (result) {
            statusIconOk(throbberStatusClassNameSelector, "Test connection is successful");
        }).fail(function (response) {
            var msg = getFailedMessage(response);
            statusIconFailed(throbberStatusClassNameSelector, msg);
        });
    }

    function getFailedMessage(response) {
        if (response.status && response.status === 401) {
            return "Jira session is ended. Relogin is required.";
        }

        var msg = !!response.responseText ? response.responseText : response.statusText;
        return msg;
    }

    function configureSpaceDialog() {
        function resetStatusIcon() {
            statusIconInit("#space-save-status");
        }

        AJS.$("#name").change(resetStatusIcon);
        AJS.$("#location").change(resetStatusIcon);
        AJS.$("#clientId").change(resetStatusIcon);
        AJS.$("#clientSecret").change(resetStatusIcon);

        AJS.$("#show-space-dialog").click(function (e) {
            e.preventDefault();
            showSpaceDialog();

        });

        AJS.$("#space-cancel-button").click(function (e) {
            e.preventDefault();
            closeSpaceDialog();
        });

        AJS.$("#dialog-test-space-connection").click(function (e) {
            e.preventDefault();
            testConnectionFromDialog();
        });


        $("#space-dialog .required").change(function () {
            validateSpaceRequiredFieldsFilled();
        });

        AJS.$("#space-submit-button").click(function (e) {
            e.preventDefault();
            octanePluginContext.spaceSaveClicked = true;
            if (!validateSpaceRequiredFieldsFilled()) {
                return;
            }

            enableSpaceSubmitButton(false);
            var editMode = !!octanePluginContext.spaceCurrentRow;
            var rowModel = editMode ? octanePluginContext.spaceCurrentRow.model.attributes : null;

            //build model
            var modelForUpdate = {
                name: $("#name").attr("value").trim(),
                location: $("#location").attr("value").trim(),
                clientId: $("#clientId").attr("value").trim(),
                clientSecret: $("#clientSecret").attr("value")
            };

            var url = octanePluginContext.spaceTable.options.resources.all;
            var requestType = "POST";
            if (editMode) {
                modelForUpdate.id = rowModel.id;
                requestType = "PUT";
                url += "/" + modelForUpdate.id;
            }

            //send
            var myJSON = JSON.stringify(modelForUpdate);
            $.ajax({
                url: url,
                type: requestType,
                data: myJSON,
                dataType: "json",
                contentType: "application/json"
            }).done(function (result) {
                if (editMode) {
                    var rowModel = octanePluginContext.spaceCurrentRow.model.attributes;
                    rowModel.name = result.name;
                    rowModel.location = result.location;
                    rowModel.clientId = result.clientId;
                    rowModel.clientSecret = result.clientSecret;
                    octanePluginContext.spaceCurrentRow.render();
                    reloadTable(octanePluginContext.workspaceTable);
                } else {//new mode
                    octanePluginContext.spaceTable.addRow(result, 0);
                }

                closeSpaceDialog();
                showSpaceStatus("Space configuration is saved successfully", true);
            }).fail(function (request, status, error) {
                $("#space-save-status").addClass(iconFailedClass);
                $("#space-save-status").attr("title", request.responseText);

                enableSpaceSubmitButton(true);
            });
        });

        function closeSpaceDialog() {
            AJS.dialog2("#space-dialog").hide();
            enableSpaceSubmitButton(true);
        }
    }

    function configureWorkspaceDialog() {
        octanePluginContext.workspaceDialogData = {};
        octanePluginContext.workspaceSaveClicked = false;

        function reloadOctaneSupportedEntityTypes() {
            $("#octaneEntityTypes").val("");//clear before in order to avoid saving not-consistent data
            var workspaceId = $("#workspaceSelector").val();
            var spaceConfId = octanePluginContext.workspaceDialogData.spaceConf.id;
            var udfName = $("#octaneUdf").attr("value");
            if (workspaceId && udfName) {
                $("#refreshOctaneEntityTypesSpinner").spin();
            } else {
                return;//don't load entity type if missing one of the workspaceId or udfName
            }

            $.ajax({
                url: octanePluginContext.octaneAdminBaseUrl + "workspaces/supported-octane-types?space-config-id=" + spaceConfId + "&workspace-id=" + workspaceId + "&udf-name=" + udfName,
                type: "GET",
                dataType: "json",
                contentType: "application/json"
            }).done(function (data) {
                setTimeout(function () {
                    $("#refreshOctaneEntityTypesSpinner").spinStop();
                    $("#octaneEntityTypes").val(data.join(", "));
                    validateWorkspaceRequiredFieldsFilled();
                    validateMissingOctaneSupportedEntityTypes(data, "#octaneEntityTypesError", udfName);
                }, 1000);
            }).fail(function (request, status, error) {
                $("#refreshOctaneEntityTypesSpinner").spinStop();
            });
        }

        $("#spaceConfSelector").change(function () {
            $("#workspaceSelector").val(null);
            fillWorkspaceDialogData($("#spaceConfSelector").select2('data').id);
        });

        $("#workspaceSelector").change(function () {
            reloadPossibleJiraFields();
        });

        $("#workspace-dialog .affect-octane-entity-types").change(function () {
            reloadOctaneSupportedEntityTypes();
        });

        $("#workspace-dialog .required").change(function () {
            validateWorkspaceRequiredFieldsFilled();
        });

        $("#jiraIssueTypesSelector").change(function () {
            clearErrorMessageIfDataIsPresent($("#jiraIssueTypesSelector").select2('data'), "#jiraIssueTypesSelectorError");
        });

        $("#refreshOctaneEntityTypesButton").click(function (e) {
            e.preventDefault();
            reloadOctaneSupportedEntityTypes();
        });

        //fixing focus on search control
        //https://community.developer.atlassian.com/t/aui-dialog-2-modal-problems-with-select-2-user-search-and-modal-does-not-lock-keyboard/10474
        $("#workspaceSelector, #spaceConfSelector").on("select2-open", function () {
            $("[tabindex=0]").attr("tabindex", "-1");
            $("div.select2-search input.select2-input").attr("tabindex", "0").focus();
        });

        AJS.$("#show-workspace-dialog").click(function (e) {
            e.preventDefault();
            showWorkspaceDialog();
        });

        function validateWorkspaceRequiredFieldsFilled() {
            if (!octanePluginContext.workspaceSaveClicked) {
                return true;
            }

            //validate
            var validationFailed = !validateMissingRequiredField($("#workspaceSelector").select2('data'), "#workspaceSelectorError");
            validationFailed = !validateMissingRequiredField($("#spaceConfSelector").select2('data'), "#spaceConfSelectorError") || validationFailed;
            validationFailed = !validateMissingRequiredField($("#octaneUdf").attr("value"), "#octaneUdfError") || validationFailed;
            validationFailed = !validateMissingRequiredField($("#octaneEntityTypes").val(), "#octaneEntityTypesError") || validationFailed;
            validationFailed = !validateMissingRequiredField($("#jiraProjectsSelector").select2('data').length, "#jiraProjectsSelectorError") || validationFailed;
            validationFailed = !validateMissingRequiredField($("#jiraIssueTypesSelector").select2('data').length, "#jiraIssueTypesSelectorError") || validationFailed;
            return !validationFailed;
        }

        function closeWorkspaceDialog() {
            AJS.dialog2("#workspace-dialog").hide();
            octanePluginContext.workspaceSaveClicked = false;
            enableWorkspaceSubmitButton(true);

            AJS.$('#workspaceSelector').val(null).trigger('change');
            AJS.$('#spaceConfSelector').val(null).trigger('change');
            AJS.$('#jiraIssueTypesSelector').val(null).trigger('change');
            AJS.$('#jiraProjectsSelector').val(null).trigger('change');

            AJS.$("#workspaceSelector").select2("destroy");
            AJS.$("#spaceConfSelector").select2("destroy");
            AJS.$("#jiraIssueTypesSelector").select2("destroy");
            AJS.$("#jiraProjectsSelector").select2("destroy");

            $("#octane-possible-fields").text("");
        }

        AJS.$("#workspace-submit-button").click(function (e) {
            e.preventDefault();
            octanePluginContext.workspaceSaveClicked = true;
            if (!validateWorkspaceRequiredFieldsFilled()) {
                return;
            }

            enableWorkspaceSubmitButton(false);

            var editMode = !!octanePluginContext.workspaceCurrentRow;
            var rowModel = editMode ? octanePluginContext.workspaceCurrentRow.model.attributes : null;

            //build model
            var modelForUpdate = {
                workspaceId: $("#workspaceSelector").select2('data').id,
                workspaceName: $("#workspaceSelector").select2('data').text,
                spaceConfigId: $("#spaceConfSelector").select2('data').id,
                spaceConfigName: $("#spaceConfSelector").select2('data').text,
                octaneUdf: $("#octaneUdf").attr("value"),
                octaneEntityTypes: ($("#octaneEntityTypes").val()) ? $("#octaneEntityTypes").attr("value").split(", ") : [], //if empty value - send empty array
                jiraIssueTypes: _.map($("#jiraIssueTypesSelector").select2('data'), function (item) {
                    return item.id;
                }),//convert selected objects to array of strings
                jiraProjects: _.map($("#jiraProjectsSelector").select2('data'), function (item) {
                    return item.id;
                }),//convert selected objects to array of strings
            };

            var url = octanePluginContext.workspaceTable.options.resources.all;
            var requestType = "POST";
            if (editMode) {
                modelForUpdate.id = rowModel.id;
                requestType = "PUT";
                url += "/" + modelForUpdate.id;
            }

            //send
            var myJSON = JSON.stringify(modelForUpdate);
            $.ajax({
                url: url,
                type: requestType,
                data: myJSON,
                dataType: "json",
                contentType: "application/json"
            }).done(function (data) {
                if (editMode) {
                    var rowModel = octanePluginContext.workspaceCurrentRow.model.attributes;
                    rowModel.workspaceId = data.workspaceId;
                    rowModel.workspaceName = data.workspaceName;
                    rowModel.spaceConfigId = data.spaceConfigId;
                    rowModel.spaceConfigName = data.spaceConfigName;
                    rowModel.octaneUdf = data.octaneUdf;
                    rowModel.octaneEntityTypes = data.octaneEntityTypes;
                    rowModel.jiraIssueTypes = data.jiraIssueTypes;
                    rowModel.jiraProjects = data.jiraProjects;
                    octanePluginContext.workspaceCurrentRow.render();
                } else {//new mode
                    octanePluginContext.workspaceTable.addRow(data, 0);
                }

                closeWorkspaceDialog();
                showWorkspaceStatus("Workspace configuration is saved successfully", true);
            }).fail(function (request, status, error) {
                showWorkspaceStatus(request.responseText, false);
                enableWorkspaceSubmitButton(true);
            });
        });

        AJS.$("#workspace-cancel-button").click(function (e) {
            e.preventDefault();
            closeWorkspaceDialog();
        });

        AJS.$("#octane-possible-fields-tooltip").dblclick(function (e) {
            e.preventDefault();
            var hasPossibleJiraField = !!octanePluginContext.workspaceDialogData.possibleJiraField;
            if (hasPossibleJiraField) {
                $("#octaneUdf").val(octanePluginContext.workspaceDialogData.possibleJiraField);
                reloadOctaneSupportedEntityTypes();
            }
        });
    }

    function disableControlsInWorkspaceDialog(disable) {
        $("#octaneUdf").prop('disabled', disable);
        $("#workspaceSelector").prop('disabled', disable);
        $("#jiraProjectsSelector").prop('disabled', disable);
        $("#jiraIssueTypesSelector").prop('disabled', disable);

        $("#workspace-submit-button").prop('disabled', disable);
    }

    function removeSpaceTooltipInWorkspaceDialog() {
        $("#spaceConfSelectorThrobber").attr("title", "");
        AJS.$('#spaceConfSelectorThrobber').tooltip('destroy');
    }

    function fillWorkspaceDialogData(spaceConfId, workspaceConfId) {
        $("#spaceConfSelectorThrobber").removeClass(iconFailedClass);
        $("#spaceConfSelectorThrobber").addClass(iconWaitClass);
        removeSpaceTooltipInWorkspaceDialog();

        disableControlsInWorkspaceDialog(true);

        return new Promise(function (resolve, reject) {
            var dataUrl = octanePluginContext.octaneAdminBaseUrl + "workspaces-dialog/additional-data?space-config-id=" + spaceConfId;
            if (workspaceConfId) {
                dataUrl = dataUrl + "&workspace-config-id=" + workspaceConfId;
            }

            $.ajax({url: dataUrl, type: "GET", dataType: "json", contentType: "application/json"})
                .done(function (data) {
                    octanePluginContext.workspaceDialogData = data;
                    octanePluginContext.workspaceDialogData.spaceConf = {id: spaceConfId};

                    fillCombo("#workspaceSelector", false, octanePluginContext.workspaceDialogData.workspaces);
                    fillCombo("#jiraIssueTypesSelector", true, octanePluginContext.workspaceDialogData.issueTypes);
                    fillCombo("#jiraProjectsSelector", true, octanePluginContext.workspaceDialogData.projects);

                    reloadPossibleJiraFields();
                    resolve();
                    disableControlsInWorkspaceDialog(false);

                    removeSpaceTooltipInWorkspaceDialog();

                    //reload the supported entity types
                    $("#refreshOctaneEntityTypesButton").click();
                }).fail(function (request) {
                var msg = !request.responseText ? request.statusText : request.responseText;
                $("#spaceConfSelectorThrobber").addClass(iconFailedClass);
                $("#spaceConfSelectorThrobber").attr("title", msg);
                AJS.$("#spaceConfSelectorThrobber").tooltip();

                reject(Error(msg));
            }).always(function () {
                $("#spaceConfSelectorThrobber").removeClass(iconWaitClass);
            });
        });
    }

    function fillEmptyCombo(selector, multiple) {
        AJS.$(selector).auiSelect2({
            multiple: multiple,
            data: {},
        });
    }

    function fillCombo(selector, multiple, data) {
        AJS.$(selector).auiSelect2({
            multiple: multiple,
            data: data,
        });
    }

    function showWorkspaceDialog(workspaceRow) {
        octanePluginContext.workspaceCurrentRow = workspaceRow;
        var editMode = !!octanePluginContext.workspaceCurrentRow;
        var rowModel = editMode ? octanePluginContext.workspaceCurrentRow.model.attributes : null;
        $("#workspace-dialog .error").text('');//clear previous error messages
        removeSpaceTooltipInWorkspaceDialog();

        disableControlsInWorkspaceDialog(true);
        fillEmptyCombo('#workspaceSelector', false);
        fillEmptyCombo('#jiraProjectsSelector', true);
        fillEmptyCombo('#jiraIssueTypesSelector', true);

        if (editMode) {//is edit mode
            $("#spaceConfSelector").val(rowModel.spaceConfigId);
            $('#spaceConfSelector').prop('disabled', true); //disable workspace selector

            $('#workspaceSelector').val([rowModel.workspaceId]);
            $('#workspaceSelector').prop('disabled', true); //disable workspace selector

            $("#octaneUdf").val(rowModel.octaneUdf);//populate default value for new item
            $("#octaneEntityTypes").val(rowModel.octaneEntityTypes.join(", "));
            $("#jiraIssueTypesSelector").val(rowModel.jiraIssueTypes);
            $("#jiraProjectsSelector").val(rowModel.jiraProjects);
            $('#workspace-dialog-title').text("Edit");//set dialog title
            AJS.dialog2("#workspace-dialog").show();
            fillWorkspaceDialogData(rowModel.spaceConfigId, rowModel.id);

        } else {//new item
            $("#octaneUdf").val("");//populate default value for new item
            $('#spaceConfSelector').prop('disabled', false);//enable space selector

            $('#workspace-dialog-title').text("Create");//set dialog title
            AJS.dialog2("#workspace-dialog").show();
        }

        var spaces = _.map(octanePluginContext.spaceTable.getModels().models, function (item) {
            return {id: item.attributes.id, text: item.attributes.name};
        });
        _.sortBy(spaces, 'text');
        AJS.$("#spaceConfSelector").auiSelect2({
            multiple: false,
            data: spaces
        });
    }

    function showSpaceDialog(rowForEdit) {

        statusIconInit("#space-save-status");

        octanePluginContext.spaceCurrentRow = rowForEdit;
        octanePluginContext.spaceSaveClicked = false;
        var isEditMode = !!rowForEdit;
        $("#space-dialog .error").text('');//clear previous error messages
        if (isEditMode) {//is edit mode
            var model = rowForEdit.model.attributes;
            $("#name").val(model.name);
            $("#location").val(model.location);
            $("#clientId").val(model.clientId);
            $("#clientSecret").val(model.clientSecret);

            $('#space-dialog-title').text("Edit");//set dialog title
        } else {//new item
            $("#name").val("");
            $("#location").val("");
            $("#clientId").val("");
            $("#clientSecret").val("");


            $('#space-dialog-title').text("Create");//set dialog title
        }

        AJS.dialog2("#space-dialog").show();
    }

    var spaceErrorFlags = [];
    var workspaceErrorFlags = [];
    var proxyErrorFlags = [];

    function enableWorkspaceSubmitButton(enable) {
        enableButton("#workspace-submit-button", enable);
    }

    function enableSpaceSubmitButton(enable) {
        enableButton("#space-submit-button", enable);
    }

    function enableButton(selector, enable) {
        if (enable) {
            AJS.$(selector).removeAttr("aria-disabled");
            AJS.$(selector).removeAttr("disabled");
        } else {
            AJS.$(selector).prop("aria-disabled", "true");
            AJS.$(selector).prop("disabled", true);
        }
    }

    function setProxyStatus(statusText, isSuccess) {
        showStatusFlag(statusText, isSuccess, proxyErrorFlags);
    }

    function showSpaceStatus(statusText, isSuccess) {
        showStatusFlag(statusText, isSuccess, spaceErrorFlags);
    }

    function showWorkspaceStatus(statusText, isSuccess) {
        showStatusFlag(statusText, isSuccess, workspaceErrorFlags);
    }

    function showStatusFlag(statusText, isSuccess, errorFlags) {
        if (isSuccess) {
            AJS.flag({type: 'success', close: 'auto', body: statusText});
            while (errorFlags.length) {
                var entry = errorFlags.pop();
                entry.close();
            }
        } else {
            var errorFlag = AJS.flag({type: 'error', close: 'manual', body: statusText});
            errorFlags.push(errorFlag);
        }
    }

    function validateMissingRequiredField(value, errorSelector) {
        return validateConditionAndUpdateErrorField(value, 'Value is missing', errorSelector);
    }

    function validateMissingOctaneSupportedEntityTypes(data, errorSelector, udfName) {
        return validateConditionAndUpdateErrorField(data.length !== 0, "No supported entities were found for mapping field: " + udfName, errorSelector);
    }

    function clearErrorMessageIfDataIsPresent(data, errorSelector) {
        return validateConditionAndUpdateErrorField(data.length !== 0, '', errorSelector);
    }

    function validateConditionAndUpdateErrorField(condition, errorMessage, errorSelector) {
        if (!condition) {
            $(errorSelector).text(errorMessage);
            return false;
        } else {
            $(errorSelector).text('');
            return true;
        }
    }

    function reloadTable(table) {
        table.$tbody.empty();
        table.fetchInitialResources();
    }

    function statusIconInit(selector) {
        var el = $(selector);
        el.removeClass(iconWaitClass);
        el.removeClass(iconOkClass);
        el.removeClass(iconFailedClass);
    }

    function statusIconStart(selector, msg) {
        var el = $(selector);
        el.addClass(iconWaitClass);
        el.removeClass(iconOkClass);
        el.removeClass(iconFailedClass);
        el.attr("title", msg);
    }

    function statusIconOk(selector, msg) {
        var el = $(selector);
        el.removeClass(iconWaitClass);
        el.addClass(iconOkClass);
        el.removeClass(iconFailedClass);
        el.attr("title", msg);
    }

    function statusIconFailed(selector, msg) {
        var el = $(selector);
        el.removeClass(iconWaitClass);
        el.removeClass(iconOkClass);
        el.addClass(iconFailedClass);
        el.attr("title", msg);
    }

})(AJS.$ || jQuery);


