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

    });

})(AJS.$ || jQuery);

function loadTable() {

    var NameReadView = AJS.RestfulTable.CustomReadView.extend({
        render: function (self) {
            return $("<strong />").text(self.value);
        }
    });
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
                alert("Edit " + instance.model.id)
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
            {id: "id", header: "<i>Id</i>", readView: NameReadView},
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
        noEntriesMsg: "No configuration",
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

        var request = $.ajax({
            url: octanePluginContext.configRestTable.options.resources.self +"/" + row.model.id,
            type: "DELETE",
        });
        request.success(function () {
            octanePluginContext.configRestTable.removeRow(row);
        });
        request.fail(function (request, status, error) {
            //TODO PRINT ERROR
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

function configureAddDialog() {
    octanePluginContext.createDialogData = {};


    function reloadOctaneSupportedEntityTypes() {
        var workspaceId = $("#workspaceSelector").val();
        var udfName = $("#octaneUdf").attr("value");
        if (workspaceId && udfName) {
            $("#refreshOctaneEntityTypesSpinner").spin();
        } else {
            return;
        }

        var request = $.ajax({
            url: octanePluginContext.octaneBaseUrl + "workspace-config/supported-octane-types?workspace-id=" + workspaceId + "&udf-name=" + udfName,
            type: "GET",
            dataType: "json",
            contentType: "application/json"
        });
        request.success(function (data) {
            console.log(data);
            setTimeout(function() {
                $("#refreshOctaneEntityTypesSpinner").spinStop();
                $("#octaneEntityTypes").val(data);
            }, 1000);
        });
        request.fail(function (request, status, error) {
            //TODO SHOW ERROR MESSAGE
            $("#refreshOctaneEntityTypesSpinner").spinStop();
        });
    }

    $("#octaneUdf").change(function () {
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

    function getAdditionalDataUrl(){
        return octanePluginContext.octaneBaseUrl + "workspace-config/additional-data";
    }

    AJS.$("#show-add-dialog").click(function (e) {
        e.preventDefault();
        var request = $.ajax({
            url: getAdditionalDataUrl(),
            type: "GET",
            dataType: "json",
            contentType: "application/json"
        });

        request.success(function (data) {
            octanePluginContext.createDialogData = data;
            AJS.$("#workspaceSelector").auiSelect2({
                multiple: false,
                //placeholder: "Select a workspace",
                data: octanePluginContext.createDialogData.workspaces,
            });

            AJS.$("#jiraIssueTypesSelector").auiSelect2({
                multiple: true,
                //placeholder: "Select issue types",
                data: octanePluginContext.createDialogData.issueTypes,
            });

            AJS.$("#jiraProjectsSelector").auiSelect2({
                multiple: true,
                //placeholder: "Select projects",
                data: octanePluginContext.createDialogData.projects,
            });

            $("#octaneUdf").val("jira_key_udf");//populate default value
            AJS.dialog2("#config-dialog").show();

        });

        request.fail(function (request, status, error) {
            //TODO SHOW ERROR MESSAGE
        });
    });

    function closeDialog() {
        AJS.dialog2("#config-dialog").hide();

        AJS.$('#workspaceSelector').val(null).trigger('change');
        AJS.$('#jiraIssueTypesSelector').val(null).trigger('change');
        AJS.$('#jiraProjectsSelector').val(null).trigger('change');

        AJS.$("#workspaceSelector").select2("destroy");
        AJS.$("#jiraIssueTypesSelector").select2("destroy");
        AJS.$("#jiraProjectsSelector").select2("destroy");
    }

    AJS.$("#dialog-submit-button").click(function (e) {
        e.preventDefault();

        setAddWorkspaceDialogStatusText("Saving...");
        var model = {};
        model.id = $("#workspaceSelector").select2('data').id;//$("#workspaceSelector").val();
        model.workspaceName = $("#workspaceSelector").select2('data').text;
        model.octaneUdf = $("#octaneUdf").attr("value");
        model.octaneEntityTypes = $("#octaneEntityTypes").attr("value").split(",");
        model.jiraIssueTypes = _.map($("#jiraIssueTypesSelector").select2('data'), function (item) {return item.id;})//convert selected objects to array of strings
        model.jiraProjects = _.map($("#jiraProjectsSelector").select2('data'), function (item) {return item.id;});//convert selected objects to array of strings

        console.log(model);
        var myJSON = JSON.stringify(model);
        var request = $.ajax({
            url: octanePluginContext.configRestTable.options.resources.self,
            type: "POST",
            data: myJSON,
            dataType: "json",
            contentType: "application/json"
        });
        request.success(function (msg) {
            octanePluginContext.configRestTable.addRow(model, 0);
            closeDialog();
        });
        request.fail(function (request, status, error) {
            setAddWorkspaceDialogStatusText(request.responseText, "statusFailed");
        });
    });

    AJS.$("#dialog-cancel-button").click(function (e) {
        e.preventDefault();
        closeDialog();
    });
}

function loadSpaceConfiguration() {
    $.ajax({
        url: octanePluginContext.octaneBaseUrl,
        dataType: "json"
    }).done(function (config) { // when the configuration is returned...
        // ...populate the form.
        $("#clientId").val(config.clientId);
        $("#clientSecret").val(config.clientSecret);
        $("#location").val(config.location);
    });
}

function buildSpaceConfigAsJson() {
    var data = {
        location: $("#location").attr("value"),
        clientId: $("#clientId").attr("value"),
        clientSecret: $("#clientSecret").attr("value"),
    }
    var myJSON = JSON.stringify(data);
    return myJSON;
}

function updateSpaceConfig() {
    setSpaceStatusText("Space configuration is saving ...");
    var data = buildSpaceConfigAsJson();
    var request = $.ajax({
        url: octanePluginContext.octaneBaseUrl,
        type: "PUT",
        data: data,
        dataType: "json",
        contentType: "application/json"
    });

    request.success(function (msg) {
        setSpaceStatusText("Space configuration is saved successfully", "statusValid");
    });

    request.fail(function (request, status, error) {
        setSpaceStatusText(request.responseText, "statusFailed");
    });
}

function setAddWorkspaceDialogStatusText(statusText, statusClass, isHtml) {
    setStatusText("#dialog-status", statusText, statusClass, isHtml)
}

function setSpaceStatusText(statusText, statusClass, isHtml) {
    setStatusText("#status", statusText, statusClass, isHtml)
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