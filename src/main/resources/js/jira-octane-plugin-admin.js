var octanePluginContext = {};
octanePluginContext.octaneBaseUrl = AJS.contextPath() + "/rest/octane-admin/1.0/";

(function ($) { // this closure helps us keep our variables to ourselves.
    // This pattern is known as an "iife" - immediately invoked function expression

    // wait for the DOM (i.e., document "skeleton") to load. This likely isn't necessary for the current case,
    // but may be helpful for AJAX that provides secondary content.
    $(document).ready(function () {

        loadTable();
        loadConfiguration();
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
                alert("Delete " + instance.model.id)
            });

            var dropdownId = "split-container-dropdown" + instance.model.id;
            var topLevelEl = $('<div class="aui-buttons">'+
                '<button class="aui-button aui-dropdown2-trigger aui-button-split-more aui-button-subtle aui-button-compact" aria-controls="'
                + dropdownId + '">...</button></div>');
            var bottomLevelEl = $('<aui-dropdown-menu id="' + dropdownId + '"></aui-dropdown-menu>').append(editButton, deleteButton);
            var parentEl = $('<div></div>').append(topLevelEl,bottomLevelEl);
            return parentEl;
            return $(buttons);
            /*return $("<a href='#' class='aui-button' />")
                .addClass(this.classNames.DELETE)
                .text("bububu").click(function (e) {
                    e.preventDefault();
                    instance.destroy();
                });*/
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
            {id: "octaneField", header: "ALM Octane Field"},
            {id: "octaneEntityTypes", header: "Supported ALM Octane Entity Types", readView: ListReadView},
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
            this.innerHTML = 'Actions';
        });
    });
}

function addButtonRegistrations() {
    AJS.$("#test_connection").click(function () {
        testConnection();
    });

    AJS.$("#save").click(function () {
        updateConfig();
    });
}

function configureAddDialog(){
    octanePluginContext.createDialogData = { };


    //fixing focus on search control
    //https://community.developer.atlassian.com/t/aui-dialog-2-modal-problems-with-select-2-user-search-and-modal-does-not-lock-keyboard/10474
    $("#workspace-selector").on("select2-open", function() {
        console.log("select2-open tabondex updated");
        $("[tabindex=0]").attr("tabindex","-1");
        $("div.select2-search input.select2-input").attr("tabindex",  "0").focus();
    });

    AJS.$("#show-dialog-button").click(function(e) {
        e.preventDefault();
        var request = $.ajax({
            url: octanePluginContext.octaneBaseUrl  + "workspace-config/additional-data",
            type: "GET",
            dataType: "json",
            contentType: "application/json"
        });

        request.success(function (data) {
            octanePluginContext.createDialogData = data;

            AJS.$("#workspaceSelector").auiSelect2({
                multiple: false,
                //placeholder: "Select a workspace",
                data : octanePluginContext.createDialogData.workspaces,
            });

            AJS.$("#jiraIssueTypesSelector").auiSelect2({
                multiple: true,
                //placeholder: "Select issue types",
                data : octanePluginContext.createDialogData.issueTypes,
            });

            AJS.$("#jiraProjectsSelector").auiSelect2({
                multiple: true,
                //placeholder: "Select projects",
                data : octanePluginContext.createDialogData.projects,
            });

            AJS.dialog2("#config-dialog").show();

        });

        request.fail(function (request, status, error) {
            //TODO SHOW ERROR MESSAGE
        });


    });

    function closeDialog(){
        AJS.dialog2("#config-dialog").hide();
        AJS.$("#workspaceSelector").select2("destroy");
        AJS.$("#jiraIssueTypesSelector").select2("destroy");
        AJS.$("#jiraProjectsSelector").select2("destroy");
    }

    AJS.$("#dialog-submit-button").click(function (e) {
        e.preventDefault();

        var model = {};
        model.id = $("#workspaceSelector").select2('data').id;//$("#workspaceSelector").val();
        model.workspaceName = $("#workspaceSelector").select2('data').text;
        model.octaneField = $("#octaneUdf").attr("value"),
        model.octaneEntityTypes = $("#octaneEntityTypes").attr("value"),
        model.jiraIssueTypes =  _.map($("#jiraIssueTypesSelector").select2('data'), function(item){return item.id;})//convert selected objects to array of strings
        model.jiraProjects = _.map($("#jiraProjectsSelector").select2('data'), function(item){return item.id;});//convert selected objects to array of strings


        console.log(model);
        //octanePluginContext.configRestTable.model.save(model);
        //octanePluginContext.configRestTable.addRow(model,0);
        console.log("added");
        closeDialog();

    });

    AJS.$("#dialog-cancel-button").click(function (e) {
        e.preventDefault();
        closeDialog();
    });
}

function loadConfiguration() {
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

function getConfigData() {
    var data = {
        location: $("#location").attr("value"),
        clientId: $("#clientId").attr("value"),
        clientSecret: $("#clientSecret").attr("value"),
    }
    var myJSON = JSON.stringify(data);
    return myJSON;
}

function updateConfig() {
    setStatusText("Configuration is saving ...");
    var data = getConfigData();
    var request = $.ajax({
        url: octanePluginContext.octaneBaseUrl,
        type: "PUT",
        data: data,
        dataType: "json",
        contentType: "application/json"
    });

    request.success(function (msg) {
        setStatusText("Configuration is saved successfully", "statusValid");
    });

    request.fail(function (request, status, error) {
        setStatusText(request.responseText, "statusFailed");
    });
}

function testConnection() {
    setStatusText("Configuration is validating ...");
    var request = $.ajax({
        url: octanePluginContext.octaneBaseUrl + "test-connection",
        type: "PUT",
        data: getConfigData(),
        dataType: "json",
        contentType: "application/json"
    });

    request.success(function (msg) {
        setStatusText("Configuration is validated successfully", "statusValid");
    });

    request.fail(function (request, status, error) {
        var response = JSON.parse(request.responseText);
        if (response.failed) {
            setStatusText(response.failed, "statusFailed");
        } else if (response.warning) {
            setStatusText(response.warning, "statusWarning", true);
        }
    });
}

function setStatusText(statusText, statusClass, isHtml) {
    $("#status").removeClass("statusValid");
    $("#status").removeClass("statusWarning");
    $("#status").removeClass("statusFailed");
    if (isHtml) {
        $("#status").html(statusText);
    } else {
        $("#status").text(statusText);
    }

    if (statusClass) {
        $("#status").addClass(statusClass);
    }
}