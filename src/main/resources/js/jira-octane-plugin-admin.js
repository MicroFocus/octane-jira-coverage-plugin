var octanePluginContext = {};
octanePluginContext.octaneBaseUrl = AJS.contextPath() + "/rest/octane-admin/1.0/";

(function ($) { // this closure helps us keep our variables to ourselves.
    // This pattern is known as an "iife" - immediately invoked function expression

    // wait for the DOM (i.e., document "skeleton") to load. This likely isn't necessary for the current case,
    // but may be helpful for AJAX that provides secondary content.
    $(document).ready(function () {
        loadTable();
        loadSpaceConfiguration();
        addButtonRegistrations();
        configureAddDialog();
        configureProxyDialog();
    });

})(AJS.$ || jQuery);

function loadTable() {
    var ListReadView = AJS.RestfulTable.CustomReadView.extend({
        render: function (self) {
            var output = _.reduce(self.value, function (memo, current) {
                return memo + '<li>' + current + '</li>';
            }, '<ul class="simple-list">');
            output += '</ul>'
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
                '<button class="aui-button aui-dropdown2-trigger aui-button-split-more aui-button-subtle aui-button-compact" aria-controls="'
                + dropdownId + '">...</button></div>');
            var bottomLevelEl = $('<aui-dropdown-menu id="' + dropdownId + '"></aui-dropdown-menu>').append(editButton, deleteButton);
            var parentEl = $('<div></div>').append(topLevelEl, bottomLevelEl);
            return parentEl;
        }
    });

    octanePluginContext.configRestTable = new AJS.RestfulTable({
        el: jQuery("#configuration-rest-table"),
        resources: {
            all: octanePluginContext.octaneBaseUrl + "workspace-config/all",
            self: octanePluginContext.octaneBaseUrl + "workspace-config/self"
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
        noEntriesMsg: "No configuration is defined",
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

function addButtonRegistrations() {
    AJS.$("#save-space-configuration").click(function () {
        updateSpaceConfig();
    });
}

function configureProxyDialog(){
    AJS.$("#show-proxy-settings").click(function (e) {
        e.preventDefault();
        setProxyDialogStatusText("");
        $.ajax({
            url: octanePluginContext.octaneBaseUrl + "proxy",
            type: "GET",
            dataType: "json",
            contentType: "application/json"
        }).done(function (data) {
            $("#proxyHost").val(data.host)
            $("#proxyPort").val(data.port)
            $("#proxyUser").val(data.username)
            $("#proxyPassword").val(data.password)

            AJS.dialog2("#proxy-dialog").show();
        });
    });

    AJS.$("#proxy-submit-button").click(function (e) {
        e.preventDefault();
        setProxyDialogStatusText("");
        var data = {
            host: $("#proxyHost").attr("value"),
            port: $("#proxyPort").attr("value"),
            username: $("#proxyUser").attr("value"),
            password: $("#proxyPassword").attr("value"),
        }

        //validation
        if(data.port && isNaN(data.port)){
            setProxyDialogStatusText("Port value must be a number.", "statusFailed");
            return;
        }

        var myJSON = JSON.stringify(data);
        $.ajax({url: octanePluginContext.octaneBaseUrl + "proxy", type: "PUT", data: myJSON, dataType: "json", contentType: "application/json"
        }).done(function (msg) {
            AJS.dialog2("#proxy-dialog").hide();
        }).fail(function (request, status, error) {
            setProxyDialogStatusText(request.responseText, "statusFailed");
        });
    });

    AJS.$("#proxy-cancel-button").click(function (e) {
        e.preventDefault();
        AJS.dialog2("#proxy-dialog").hide();
    });
}

function configureAddDialog() {
    octanePluginContext.createDialogData = {};

    function reloadOctaneSupportedEntityTypes() {
        var workspaceId = $("#workspaceSelector").val();
        var udfName = $("#octaneUdf").attr("value");
        if (workspaceId && udfName) {
            $("#refreshOctaneEntityTypesSpinner").spin();
        } else {
            return;//dont load entity type if missing one of the workspaceId or udfName
        }

        $.ajax({
            url: octanePluginContext.octaneBaseUrl + "workspace-config/supported-octane-types?workspace-id=" + workspaceId + "&udf-name=" + udfName,
            type: "GET",
            dataType: "json",
            contentType: "application/json"
        }).done(function (data) {
            setTimeout(function() {
                $("#refreshOctaneEntityTypesSpinner").spinStop();
                $("#octaneEntityTypes").val(data);
            }, 1000);
        }).fail(function (request, status, error) {
            $("#refreshOctaneEntityTypesSpinner").spinStop();
        });
    }

    $("#octaneUdf").change(function () {
        reloadOctaneSupportedEntityTypes();
    });

    $("#workspaceSelector").change(function () {
        reloadOctaneSupportedEntityTypes();
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

    function closeDialog() {
        AJS.dialog2("#config-dialog").hide();

        setWorkspaceDialogStatusText("");//clear

        AJS.$('#workspaceSelector').val(null).trigger('change');
        AJS.$('#jiraIssueTypesSelector').val(null).trigger('change');
        AJS.$('#jiraProjectsSelector').val(null).trigger('change');

        AJS.$("#workspaceSelector").select2("destroy");
        AJS.$("#jiraIssueTypesSelector").select2("destroy");
        AJS.$("#jiraProjectsSelector").select2("destroy");
    }

    AJS.$("#dialog-submit-button").click(function (e) {
        e.preventDefault();

        var modelForUpdate = {};
        modelForUpdate.id = $("#workspaceSelector").select2('data').id;//$("#workspaceSelector").val();
        modelForUpdate.workspaceName = $("#workspaceSelector").select2('data').text;
        modelForUpdate.octaneUdf = $("#octaneUdf").attr("value");
        modelForUpdate.octaneEntityTypes = $("#octaneEntityTypes").attr("value").split(",");
        modelForUpdate.jiraIssueTypes = _.map($("#jiraIssueTypesSelector").select2('data'), function (item) {return item.id;})//convert selected objects to array of strings
        modelForUpdate.jiraProjects = _.map($("#jiraProjectsSelector").select2('data'), function (item) {return item.id;});//convert selected objects to array of strings

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
        }).fail(function (request, status, error) {
            setWorkspaceDialogStatusText(request.responseText, "statusFailed");
        });
    });

    AJS.$("#dialog-cancel-button").click(function (e) {
        e.preventDefault();
        closeDialog();
    });
}

function showWorkspaceConfigDialog(){

    var dataUrl = octanePluginContext.octaneBaseUrl + "workspace-config/additional-data";
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
        //$("#octaneUdf").val("jira_key_udf");//populate default value for new item
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

            AJS.dialog2("#config-dialog").show();
    });
}

function loadSpaceConfiguration() {
    $.ajax({url: octanePluginContext.octaneBaseUrl, dataType: "json"})
        .done(function (config) { // when the configuration is returned...
        // ...populate the form.
        $("#clientId").val(config.clientId);
        $("#clientSecret").val(config.clientSecret);
        $("#location").val(config.location);
    });
}


function updateSpaceConfig() {

    setSpaceStatusText("Space configuration is saving ...");

    var data = {
        location: $("#location").attr("value"),
        clientId: $("#clientId").attr("value"),
        clientSecret: $("#clientSecret").attr("value"),
    }

    var data = JSON.stringify(data);
    $.ajax({url: octanePluginContext.octaneBaseUrl, type: "PUT", data: data, dataType: "json", contentType: "application/json"
    }).done(function (msg) {
        setSpaceStatusText("Space configuration is saved successfully", "statusValid");
    }).fail(function (request, status, error) {
        setSpaceStatusText(request.responseText, "statusFailed");
    });
}

function setProxyDialogStatusText(statusText, statusClass, isHtml) {
    setStatusText("#proxyStatus", statusText, statusClass, isHtml)
}

function setWorkspaceDialogStatusText(statusText, statusClass, isHtml) {
    setStatusText("#workspaceStatus", statusText, statusClass, isHtml)
}

function setSpaceStatusText(statusText, statusClass, isHtml) {
    setStatusText("#spaceStatus", statusText, statusClass, isHtml)
}

function setStatusText(selector, statusText, statusClass, isHtml) {
    $(selector).removeClass("statusValid");
    $(selector).removeClass("statusWarning");
    $(selector).removeClass("statusFailed");
    if (isHtml) {
        $(selector).html(statusText);
    } else {
        $(selector).text(statusText);
    }

    if (statusClass) {
        $(selector).addClass(statusClass);
    }
}