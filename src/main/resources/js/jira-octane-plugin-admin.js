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
            console.log(self);
            return $("<strong />").text(self.value);
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
            {id: "octaneField", header: "Octane Field"}
        ],
        autoFocus: false,
        allowEdit: false,
        allowCreate: false,
        noEntriesMsg: "No configuration",
        loadingMsg: "Loading",
        allowDelete: true,
        deleteConfirmation: function (model) {
            return '<section role="dialog" id="cep-confirm-delete-dialog" class="aui-dialog2 aui-dialog2-small aui-dialog2-warning">' +
                '	<header class="aui-dialog2-header"><h2 class="aui-dialog2-header-main">Delete?</h2></header>' +
                '	<div class="aui-dialog2-content"><p>Do you really want to delete configuration for workspace ' + model.workspaceName + '</p></div>' +
                '   <footer class="aui-dialog2-footer">' +
                '   <div class="aui-dialog2-footer-actions">'+
                '		<form style="display: inline; margin: 0 5px;"><input type="submit" id="dialog-submit-button" class="aui-button aui-button-primary" value="Yes"/></form>' +
                '       <button id="warning-dialog-cancel" class="aui-button aui-button-link cancel" >Cancel</button>'+
                '   </div></footer></section>';
        },

        deleteConfirmationCallback2 : function (model) {
            AJS.$("#restful-table-model")[0].innerHTML = "<b>ID:</b> " + model.id + " <b>status:</b> " + model.status + " <b>description:</b> " + model.description;
            AJS.dialog2("#delete-confirmation-dialog").show();
            return new Promise(function (resolve, reject) {
                AJS.$("#dialog-submit-button").click(function (e) {
                    resolve();
                    e.preventDefault();
                    AJS.dialog2("#delete-confirmation-dialog").hide();
                });
                AJS.$(".aui-dialog2-header-close, #warning-dialog-cancel").click(function (e) {
                    reject();
                    e.preventDefault();
                    AJS.dialog2("#delete-confirmation-dialog").hide();
                });
            });
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