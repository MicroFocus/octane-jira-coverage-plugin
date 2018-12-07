(function ($) { // this closure helps us keep our variables to ourselves.
    // This pattern is known as an "iife" - immediately invoked function expression

    // form the URL
    var url = AJS.contextPath() + "/rest/octane-admin/1.0/";

    // wait for the DOM (i.e., document "skeleton") to load. This likely isn't necessary for the current case,
    // but may be helpful for AJAX that provides secondary content.
    $(document).ready(function() {


        // request the config information from the server
        $.ajax({
            url: url,
            dataType: "json"
        }).done(function(config) { // when the configuration is returned...
            // ...populate the form.
            $("#location").val(config.location);
            $("#client_id").val(config.client_id);
            $("#client_secret").val(config.client_secret);

            AJS.$("#save").submit(function(e) {
                alert('save config');
                e.preventDefault();
                updateConfig();
            });
            AJS.$("#test_connection").submit(function(e) {
                alert('test_connection');
            });
        });
    });

})(AJS.$ || jQuery);

function updateConfig() {
    AJS.$.ajax({
        url: AJS.contextPath() + "/rest/octane-admin/1.0/",
        type: "PUT",
        contentType: "application/json",
        data: '{ "location": "' + AJS.$("#location").attr("value") + '", "client_id": ' +  AJS.$("#client_id").attr("value") + '"client_secret": ' +  AJS.$("#client_secret").attr("value") +'" }',
        processData: false
    });
}