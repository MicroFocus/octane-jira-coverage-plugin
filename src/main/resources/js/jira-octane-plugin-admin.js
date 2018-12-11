var baseUrl = AJS.contextPath() + "/rest/octane-admin/1.0/";

(function ($) { // this closure helps us keep our variables to ourselves.
    // This pattern is known as an "iife" - immediately invoked function expression

    // wait for the DOM (i.e., document "skeleton") to load. This likely isn't necessary for the current case,
    // but may be helpful for AJAX that provides secondary content.
    $(document).ready(function () {
        // request the config information from the server
        $.ajax({
            url: baseUrl,
            dataType: "json"
        }).done(function (config) { // when the configuration is returned...
            // ...populate the form.
            $("#clientId").val(config.clientId);
            $("#clientSecret").val(config.clientSecret);
            $("#location").val(config.location);
            $("#octaneUdf").val(config.octaneUdf);

            $("#test_connection").click(function () {
                testConnection();
            });

            $("#save").click(function () {
                updateConfig();
            });
        });
    });

})(AJS.$ || jQuery);

function getData() {
    var data = {
        location : $("#location").attr("value"),
        clientId : $("#clientId").attr("value"),
        clientSecret : $("#clientSecret").attr("value"),
        octaneUdf : $("#octaneUdf").attr("value")

    }
    var myJSON = JSON.stringify(data);
    return myJSON;
}

function updateConfig() {
    setStatusText("Configuration is saving ...");
    var data = getData();
    var request = $.ajax({
        url: baseUrl,
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
        url: baseUrl + "test-connection",
        type: "PUT",
        data: getData(),
        dataType: "json",
        contentType: "application/json"
    });

    request.success(function (msg) {
        setStatusText("Configuration is validated successfully", "statusValid");
    });

    request.fail(function (request, status, error) {
        setStatusText(request.responseText, "statusFailed");
    });
}

function setStatusText(statusText, statusClass) {
    $("#status").removeClass("statusValid");
    $("#status").removeClass("statusFailed");
    $("#status").text(statusText);
    if (statusClass) {
        $("#status").addClass(statusClass);
    }
}