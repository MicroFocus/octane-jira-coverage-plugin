var baseUrl = AJS.contextPath() + "/rest/octane-admin/1.0/";

(function ($) { // this closure helps us keep our variables to ourselves.
    // This pattern is known as an "iife" - immediately invoked function expression

    // wait for the DOM (i.e., document "skeleton") to load. This likely isn't necessary for the current case,
    // but may be helpful for AJAX that provides secondary content.
    $(document).ready(function() {

        // request the config information from the server
        $.ajax({
            url: baseUrl,
            dataType: "json"
        }).done(function(config) { // when the configuration is returned...
            // ...populate the form.
            $("#client_id").val(config.client_id);
            $("#client_secret").val(config.client_secret);
            $("#location").val(config.location);

            $( "#test_connection" ).click(function() {
                testConnection();
            });

            $("#save").submit(function(e) {
                e.preventDefault();
                updateConfig();
            });
        });
    });

})(AJS.$ || jQuery);

function getData(){
    var data = '{"location":"'+  $("#location").attr("value")+'","client_id":"'+  $("#client_id").attr("value")+'","client_secret":"'+$("#client_secret").attr("value")  +'"}';
    return data;
}

function updateConfig() {

    var request = $.ajax({
        url: baseUrl,
        type: "PUT",
        data: getData(),
        dataType: "json",
        contentType: "application/json"
    });

    request.done(function( msg ) {
        $("#status").val("done");
    });

    request.fail(function( jqXHR, textStatus ) {
        $("#status").val( "Request failed: " + textStatus );
    });
}

function testConnection() {
    /*$("#location").val("a1_" + data);
    $.ajax({
        url: baseUrl +"test-connection",
        type: "PUT",
        contentType: "application/json",
        dataType: "json",
        data: data ,
        processData: false
    }).done(function(config) { // when the configuration is returned...
        $("#location").val("a2_" + data);
    });*/

    var request = $.ajax({
        url: baseUrl +"test-connection",
        type: "PUT",
        data: getData(),
        dataType: "json",
        contentType: "application/json"
    });

    request.done(function( msg ) {
        $("#status").text("done");
    });

    request.fail(function( jqXHR, textStatus ) {
        $("#status").text( "Request failed: " + textStatus );
    });
}