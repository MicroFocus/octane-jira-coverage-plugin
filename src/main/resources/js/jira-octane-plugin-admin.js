
(function ($) { // this closure helps us keep our variables to ourselves.
    // This pattern is known as an "iife" - immediately invoked function expression
    var octanePluginContext = {};
    octanePluginContext.octaneAdminBaseUrl = AJS.contextPath() + "/rest/octane-admin/1.0/";


    // wait for the DOM (i.e., document "skeleton") to load. This likely isn't necessary for the current case,
    // but may be helpful for AJAX that provides secondary content.
    $(document).ready(function () {
        window.onbeforeunload = null;//Disable “Changes you made may not be saved” pop-up window
        configureWorkspaceConfigurationTable();
        loadSpaceConfiguration();
        configureSpaceButtons();
        configureWorkspaceConfigurationDialog();
        configureProxyDialog();
    });


    function configureWorkspaceConfigurationTable() {
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


                var editButton = $('<aui-item-link >Edit</aui-item-link>').click(function (e) {
                    octanePluginContext.currentRow = instance;
                    showWorkspaceConfigDialog();
                });
                var deleteButton = $('<aui-item-link >Remove</aui-item-link>').click(function (e) {
                    removeRow(instance);
                });

                //add action button
                var dropdownId = "split-container-dropdown" + instance.model.id;
                var topLevelEl = $('<div class="aui-buttons">' +
                    '<button class="aui-button aui-dropdown2-trigger aui-button-split-more aui-button-subtle aui-button-compact" aria-controls="' + dropdownId + '">...</button></div>');
                var bottomLevelEl = $('<aui-dropdown-menu id="' + dropdownId + '"></aui-dropdown-menu>').append(editButton, deleteButton);
                var parentEl = $('<div></div>').append(topLevelEl, bottomLevelEl);
                return parentEl;
            }
        });

        octanePluginContext.configRestTable = new AJS.RestfulTable({
            el: jQuery("#configuration-rest-table"),
            resources: {
                all: octanePluginContext.octaneAdminBaseUrl + "workspace-config/all",
                self: octanePluginContext.octaneAdminBaseUrl + "workspace-config/self"
            },
            columns: [
                {id: "id", header: "Workspace Id"},
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

        AJS.$(document).bind(AJS.RestfulTable.Events.INITIALIZED, function () {
            //update name of action column that is second from end
            //last two columns don't have name : action column and loading indicator used when editing
            $("#configuration-rest-table th:nth-last-child(2)").each(function () {
                //this.innerHTML = 'Actions';
            });
        });
    }

    function removeRow(row){
        $("#workspace-to-delete").text(row.model.attributes.workspaceName);//update workspace name in dialog text

        AJS.dialog2("#warning-dialog").show();
        AJS.$("#warning-dialog-confirm").click(function (e) {
            e.preventDefault();
            AJS.dialog2("#warning-dialog").hide();

            $.ajax({url: octanePluginContext.configRestTable.options.resources.self +"/" + row.model.id, type: "DELETE",
            }).done(function () {
                octanePluginContext.configRestTable.removeRow(row);
            });
        });

        AJS.$("#warning-dialog-cancel").click(function (e) {
            e.preventDefault();
            AJS.dialog2("#warning-dialog").hide();
        });
    }

    function configureProxyDialog(){
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
            };

            //validation
            if (data.host) {
                var validationFailed = false;
                validationFailed = !validateConditionAndUpdateErrorField(data.host.toLowerCase().indexOf("http") !== 0,
                    "Enter host name without http://", "#proxyHostError") || validationFailed;
                validationFailed =
                    !validateConditionAndUpdateErrorField((data.port && !isNaN(data.port)), "Port value must be a number.", "#proxyPortError") ||
                    !validateConditionAndUpdateErrorField((data.port >= 0 && data.port <= 65535), "Port must range from 0 to 65,535.", "#proxyPortError") ||
                    validationFailed;

                if (validationFailed) {
                    return;
                }
            }

            var myJSON = JSON.stringify(data);
            $.ajax({url: octanePluginContext.octaneAdminBaseUrl + "proxy", type: "PUT", data: myJSON, dataType: "json", contentType: "application/json"
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
            console.log("setTitle : " + text);
            $("#octane-possible-fields-tooltip").attr("title", text);
            AJS.$("#octane-possible-fields-tooltip").tooltip();
            $("#octane-possible-fields-tooltip").toggleClass("aui-iconfont-info-filled", filled);

        }

        var workspaceId = $("#workspaceSelector").val();
        if (workspaceId) {
            setTitle("Searching...", false);
            $.ajax({
                url: octanePluginContext.octaneAdminBaseUrl + "workspace-config/possible-jira-fields?workspace-id=" + workspaceId,
                type: "GET",
                dataType: "json",
                contentType: "application/json"
            }).done(function (data) {
                if (data && data.length) {
                    setTitle("Suggested ALM Octane fields: " + data.join(",  "), true);
                } else {
                    setTitle("No suggested fields are found.");
                }
            }).fail(function (request, status, error) {
                setTitle.text("Failed to fetch suggested fields from ALM Octane: " + request.responseText, true);
                console.error(request.responseText);
            });
        } else {
            setTitle("Select a workspace to show the list of suggested mapping fields that include 'jira' in their name.", false);
        }
    }

    function configureWorkspaceConfigurationDialog() {
        octanePluginContext.createDialogData = {};
        octanePluginContext.saveClicked = false;

        function reloadOctaneSupportedEntityTypes() {
            $("#octaneEntityTypes").val("");//clear before in order to avoid saving not-consistent data
            var workspaceId = $("#workspaceSelector").val();
            var udfName = $("#octaneUdf").attr("value");
            if (workspaceId && udfName) {
                $("#refreshOctaneEntityTypesSpinner").spin();
            } else {
                return;//don't load entity type if missing one of the workspaceId or udfName
            }

            $.ajax({
                url: octanePluginContext.octaneAdminBaseUrl + "workspace-config/supported-octane-types?workspace-id=" + workspaceId + "&udf-name=" + udfName,
                type: "GET",
                dataType: "json",
                contentType: "application/json"
            }).done(function (data) {
                setTimeout(function() {
                    $("#refreshOctaneEntityTypesSpinner").spinStop();
                    $("#octaneEntityTypes").val(data);
                    validateRequiredFieldsFilled();
                }, 1000);
            }).fail(function (request, status, error) {
                $("#refreshOctaneEntityTypesSpinner").spinStop();
            });
        }

        $("#workspaceSelector").change(function () {
            reloadPossibleJiraFields();
        });

        $("#config-dialog .affect-octane-entity-types").change(function () {
            reloadOctaneSupportedEntityTypes();
        });

        $("#config-dialog .required").change(function () {
            validateRequiredFieldsFilled();
        });

        $("#refreshOctaneEntityTypesButton").click(function (e) {
            e.preventDefault();
            reloadOctaneSupportedEntityTypes();
        });

        //fixing focus on search control
        //https://community.developer.atlassian.com/t/aui-dialog-2-modal-problems-with-select-2-user-search-and-modal-does-not-lock-keyboard/10474
        $("#workspaceSelector").on("select2-open", function () {
            $("[tabindex=0]").attr("tabindex", "-1");
            $("div.select2-search input.select2-input").attr("tabindex", "0").focus();
        });

        AJS.$("#show-add-dialog").click(function (e) {
            e.preventDefault();
            octanePluginContext.currentRow = null;
            showWorkspaceConfigDialog();
        });

        function validateRequiredFieldsFilled(){
            if(!octanePluginContext.saveClicked){
                return true;
            }

            //validate
            var validationFailed = !validateMissingRequiredField($("#workspaceSelector").select2('data'), "#workspaceSelectorError");
            validationFailed = !validateMissingRequiredField($("#octaneUdf").attr("value"), "#octaneUdfError") || validationFailed;
            validationFailed = !validateMissingRequiredField($("#octaneEntityTypes").val(), "#octaneEntityTypesError") || validationFailed;
            validationFailed = !validateMissingRequiredField($("#jiraProjectsSelector").select2('data').length, "#jiraProjectsSelectorError") || validationFailed;
            validationFailed = !validateMissingRequiredField($("#jiraIssueTypesSelector").select2('data').length, "#jiraIssueTypesSelectorError") || validationFailed;
            return !validationFailed;
        }

        function closeDialog() {
            AJS.dialog2("#config-dialog").hide();
            octanePluginContext.saveClicked = false;

            AJS.$('#workspaceSelector').val(null).trigger('change');
            AJS.$('#jiraIssueTypesSelector').val(null).trigger('change');
            AJS.$('#jiraProjectsSelector').val(null).trigger('change');

            AJS.$("#workspaceSelector").select2("destroy");
            AJS.$("#jiraIssueTypesSelector").select2("destroy");
            AJS.$("#jiraProjectsSelector").select2("destroy");

            $("#octane-possible-fields").text("");
        }

        AJS.$("#dialog-submit-button").click(function (e) {
            e.preventDefault();
            octanePluginContext.saveClicked = true;
            if (!validateRequiredFieldsFilled()) {
                return;
            }

            //build model
            var modelForUpdate = {};
            modelForUpdate.id = $("#workspaceSelector").select2('data').id;
            modelForUpdate.workspaceName = $("#workspaceSelector").select2('data').text;
            modelForUpdate.octaneUdf = $("#octaneUdf").attr("value");
            modelForUpdate.octaneEntityTypes = ($("#octaneEntityTypes").val()) ? $("#octaneEntityTypes").attr("value").split(",") : []; //if empty value - send empty array
            modelForUpdate.jiraIssueTypes = _.map($("#jiraIssueTypesSelector").select2('data'), function (item) {return item.id;});//convert selected objects to array of strings
            modelForUpdate.jiraProjects = _.map($("#jiraProjectsSelector").select2('data'), function (item) {return item.id;});//convert selected objects to array of strings

            //send
            var myJSON = JSON.stringify(modelForUpdate);
            $.ajax({url: octanePluginContext.configRestTable.options.resources.self, type: "POST", data: myJSON, dataType: "json", contentType: "application/json"
            }).done(function (msg) {
                if (octanePluginContext.currentRow) {//is edit mode
                    var rowModel = octanePluginContext.currentRow.model.attributes;
                    rowModel.octaneUdf = modelForUpdate.octaneUdf;
                    rowModel.octaneEntityTypes = modelForUpdate.octaneEntityTypes;
                    rowModel.jiraIssueTypes = modelForUpdate.jiraIssueTypes;
                    rowModel.jiraProjects = modelForUpdate.jiraProjects;
                    octanePluginContext.currentRow.render();
                } else {//new mode
                    octanePluginContext.configRestTable.addRow(modelForUpdate, 0);
                }

                closeDialog();
                showWorkspaceStatus("Workspace configuration is saved successfully", true);
            }).fail(function (request, status, error) {
                showWorkspaceStatus(request.responseText, false);
            });
        });

        AJS.$("#dialog-cancel-button").click(function (e) {
            e.preventDefault();
            closeDialog();
        });
    }

    function showWorkspaceConfigDialog(){

        var dataUrl = octanePluginContext.octaneAdminBaseUrl + "workspace-config/additional-data";
        $("#config-dialog .error").text('');//clear previous error messages
        if(octanePluginContext.currentRow){//is edit mode
            var model = octanePluginContext.currentRow.model.attributes;
            dataUrl = dataUrl + "?update-workspace-id=" + model.id;
            $('#workspaceSelector').val([model.id]);
            $('#workspaceSelector').prop('disabled', true); //disable workspace selector
            $("#octaneUdf").val(model.octaneUdf);//populate default value for new item
            $("#octaneEntityTypes").val(model.octaneEntityTypes);
            $("#jiraIssueTypesSelector").val(model.jiraIssueTypes);
            $("#jiraProjectsSelector").val(model.jiraProjects);

            $('#config-dialog-title').text("Edit");//set dialog title
        } else {//new item
            $("#octaneUdf").val("");//populate default value for new item
            $('#workspaceSelector').prop('disabled', false);//enable workspace selector

            $('#config-dialog-title').text("Create");//set dialog title
        }

        $.ajax({url: dataUrl, type: "GET", dataType: "json", contentType: "application/json"})
            .done(function (data) {
                octanePluginContext.createDialogData = data;
                AJS.$("#workspaceSelector").auiSelect2({
                    multiple: false,
                    data: octanePluginContext.createDialogData.workspaces
                });

                AJS.$("#jiraIssueTypesSelector").auiSelect2({
                    multiple: true,
                    data: octanePluginContext.createDialogData.issueTypes,
                });

                AJS.$("#jiraProjectsSelector").auiSelect2({
                    multiple: true,
                    data: octanePluginContext.createDialogData.projects,
                });
                AJS.$("#newSelector").auiSelect2({
                    multiple: false,
                    maximumselectionsize:1,
                    tags:["red", "green", "blue"]
                });

                reloadPossibleJiraFields();
                AJS.dialog2("#config-dialog").show();
            });
    }

    function loadSpaceConfiguration() {
        $.ajax({url: octanePluginContext.octaneAdminBaseUrl, dataType: "json"})
            .done(function (config) { // when the configuration is returned...
                // ...populate the form.
                $("#clientId").val(config.clientId);
                $("#clientSecret").val(config.clientSecret);
                $("#location").val(config.location);
            });
    }

    function configureSpaceButtons(){
        AJS.$("#save-space-configuration").click(function () {
            updateSpaceConfig();
        });

        function updateSpaceConfig() {
            var data = {
                location: $("#location").attr("value"),
                clientId: $("#clientId").attr("value"),
                clientSecret: $("#clientSecret").attr("value"),
            };

            var dataJson = JSON.stringify(data);
            $('.space-save-status').removeClass("aui-iconfont-successful-build");
            $('.space-save-status').removeClass("aui-iconfont-error");

            $("#reloadSpinner").spin();

            $.ajax({url: octanePluginContext.octaneAdminBaseUrl, type: "PUT", data: dataJson, dataType: "json", contentType: "application/json"
            }).done(function (msg) {
                showSpaceStatus("Space configuration is saved successfully", true);
                $('.space-save-status').addClass("aui-iconfont-successful-build");
                $("#reloadSpinner").spinStop();

            }).fail(function (request, status, error) {
                console.log(status);
                var msg = request.responseText;
                if(!msg && status && status === 'timeout'){
                    msg = "Timeout : possibly proxy settings are missing.";
                }

                showSpaceStatus(msg, false);
                $('.space-save-status').addClass("aui-iconfont-error");
                $("#reloadSpinner").spinStop();

            });
        }
    }



    var spaceErrorFlags = [];
    var workspaceErrorFlags = [];
    var proxyErrorFlags = [];

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
                var entry = spaceErrorFlags.pop();
                console.log(entry);
                entry.close();
            }
        } else {
            var errorFlag = AJS.flag({type: 'error', close: 'manual', body: statusText});
            errorFlags.push(errorFlag);
        }
    }

    function validateMissingRequiredField(value, errorSelector){
        return validateConditionAndUpdateErrorField(value, 'Value is missing', errorSelector);
    }

    function validateConditionAndUpdateErrorField(condition, errorMessage, errorSelector){
        if (!condition) {
            $(errorSelector).text(errorMessage);
            return false;
        } else {
            $(errorSelector).text('');
            return true;
        }
    }
})(AJS.$ || jQuery);


