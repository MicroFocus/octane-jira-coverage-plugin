var octanePluginContext = {};
octanePluginContext.coverageBaseUrl = AJS.contextPath() + "/rest/octane-coverage/1.0/";

function initDatePicker() {

     if (!AJS.$('#run-start-date-picker').attr("data-aui-dp-uuid")) {
        document.getElementById("run-start-date-picker").max = new Date().toISOString().split("T")[0];
        AJS.$("#run-start-date-picker").datePicker({
            overrideBrowserDefault: true,
            placeholder:"yyyy-mm-dd",
            onSelect: datePickerOnSelect,
        });
    }
}

function datePickerOnSelect(newDate) {
    closeDatePickerDialog();
    console.log(newDate);
    $('#run-start-date-picker').removeAttr("value");

    if (newDate) {//has filter
        $("#run-start-date-picker").attr("value", newDate);
        $('#show-start-date-picker').removeClass("aui-iconfont-calendar");
        $('#show-start-date-picker').addClass("aui-iconfont-calendar-filled");
    } else {
        $('#show-start-date-picker').addClass("aui-iconfont-calendar");
        $('#show-start-date-picker').removeClass("aui-iconfont-calendar-filled");
        removeDatePicker();
    }

    //build url with query string
    $("#reloadSpinner").spin();
    var query = $("#filterQueryString").attr("value") + "&filter-date=" + newDate;
    var url = octanePluginContext.coverageBaseUrl + "coverage?" + query;

    //do request
    $.ajax({
        url: url,
        type: "GET",
        dataType: "json",
        contentType: "application/json"
    }).done(function (data) {
        var totalRuns = 0;
        data.forEach(function (entry) {
            var idSelector = "#" + entry.fields.id;
            var countSelector = idSelector + " .octane-test-status-count";
            var percentageSelector = idSelector + " .octane-test-status-percentage";
            $(idSelector).removeClass();
            $(idSelector).addClass(entry.fields.className);
            $(countSelector).text(entry.fields.countStr);
            $(percentageSelector).text(entry.fields.percentage);
            totalRuns = totalRuns + entry.fields.countInt;
        });

        $("#total-runs-row .octane-test-status-total-count").text(totalRuns);

        setTimeout(function () {
            $("#reloadSpinner").spinStop();
        }, 1000);

    }).fail(function (request, status, error) {
        $("#reloadSpinner").spinStop();
    });
}

function addButtonsToDatePicker() {
    var datePickerPopupEl = getDatePickerPopupElement();
    if (!datePickerPopupEl.find('#custom-datepicker-buttons').length) {
        var closeButton = $('<button id="custom-datepicker-close-button">Close</button>').click(function (e) {
            closeDatePickerDialog();
        });
        var clearSelectionButton = $('<button id="custom-datepicker-clear-button">Clear</button>').click(function (e) {
            datePickerOnSelect("");
        });
        var buttonContainer = $('<div id="custom-datepicker-buttons"></div>').append(closeButton, clearSelectionButton);
        datePickerPopupEl.append(buttonContainer);
    }
}

function getDatePickerPopupElement() {
    console.log(("getDatepicketPopupElement"));
    var uuid = AJS.$('#run-start-date-picker').attr("data-aui-dp-uuid");

    //jira version 7 - search direct popup

    var popupEl = AJS.$('.hasDatepicker[data-aui-dp-popup-uuid="' + uuid + '"]');

    //jira version 8  -search by parent
    if (popupEl.length === 0) {
        popupEl = AJS.$('.aui-datepicker-dialog[id="' + uuid + '"] .hasDatepicker');
        console.log(("jira 8"));
    } else {
        console.log(("jira 7"));
    }

    return popupEl;
}

function getDatePickerParentElement(){
    console.log(("getDatePickerParentElement"));
    var parent = getDatePickerPopupElement().parent();
    if(parent.hasClass("aui-datepicker-dialog")){
        return parent; //version 8
    }else{
        return parent.parent();//version 7
    }
}

function closeDatePickerDialog() {
    //var datepickerDialogEl = getDatePickerParentElement();
    //$('#run-start-date-picker').removeClass("active");
    removeDatePicker();
}

function removeDatePicker() {
    //remove dialog from dom
    var datepickerDialogEl = getDatePickerParentElement().remove();
    datepickerDialogEl.css("display", "none");

    //remove data-aui-dp-uuid and value from input element

    $('#run-start-date-picker').removeAttr("data-aui-dp-uuid");
}

function showRunStartDatePicker() {
    initDatePicker();
    $("#run-start-date-picker").focus();
    setTimeout(function () {
        addButtonsToDatePicker();
    }, 100);
}

