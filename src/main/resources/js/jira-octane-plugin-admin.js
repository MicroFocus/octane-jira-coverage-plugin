var octaneBaseUrl = AJS.contextPath() + "/rest/octane-admin/1.0/";
var configRestTable

(function ($) { // this closure helps us keep our variables to ourselves.
    // This pattern is known as an "iife" - immediately invoked function expression

    // wait for the DOM (i.e., document "skeleton") to load. This likely isn't necessary for the current case,
    // but may be helpful for AJAX that provides secondary content.
    $(document).ready(function () {

        loadTable();
        loadConfiguration();
        addButtonRegistrations();
    });

})(AJS.$ || jQuery);

function loadTable() {

    var NameReadView = AJS.RestfulTable.CustomReadView.extend({
        render: function (self) {
            //console.log(self);
            //var row = this;
            console.log(this.model);
            return $("<strong />").text(self.value);
        }
    });
    var ListReadView = AJS.RestfulTable.CustomReadView.extend({
        render: function (self) {
            var output = _.reduce(self.value, function(memo, current) {
                return memo + '<li>' + current + '</li>';
            },'<ul class="simple-list">');
            output += '</ul>'
            return output;
        }
    });

    var MyRow = AJS.RestfulTable.Row.extend({
        renderOperations: function () {
            var instance = this;

            return $("<a href='#' class='aui-button' />")
                .addClass(this.classNames.DELETE)
                .text("bububu").click(function (e) {
                    e.preventDefault();
                    instance.destroy();
                });

        }
    });


    configRestTable = new AJS.RestfulTable({
        el: jQuery("#configuration-rest-table"),
        resources: {
            all: octaneBaseUrl + "all",
            self: octaneBaseUrl + "self"
        },
        columns: [
            {id: "id", header: "Id",readView: NameReadView},
            {id: "workspaceName", header: "Workspace Name"},
            {id: "octaneField", header: "ALM Octane Field"},
            {id: "octaneEntityTypes", header: "Supported ALM Octane Entity Types", readView: ListReadView},
            {id: "jiraIssueTypes", header: "Jira Issue Types", readView: ListReadView},
            {id: "jiraProject", header: "Jira Project", readView: ListReadView}
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
}

function addButtonRegistrations() {
    $("#test_connection").click(function () {
        testConnection();
    });

    $("#save").click(function () {
        updateConfig();
    });
}

function loadConfiguration() {
    $.ajax({
        url: octaneBaseUrl,
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
        url: octaneBaseUrl,
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
        url: octaneBaseUrl + "test-connection",
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