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

    $("#run-start-date-picker").val(newDate);
    if (newDate) {//has filter
        $('#show-start-date-picker').removeClass("aui-iconfont-calendar");
        $('#show-start-date-picker').addClass("aui-iconfont-calendar-filled");
    } else {
        $('#show-start-date-picker').addClass("aui-iconfont-calendar");
        $('#show-start-date-picker').removeClass("aui-iconfont-calendar-filled");
        clearDatePickerSelection();
    }

    //build url with query string
    $("#reloadSpinner").spin();
    var query = $("#filterQueryString").attr("value") + "&filter-date=" + newDate
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
    var datePickerPopupEl = getDatepicketPopupElement();
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

function getDatepicketPopupElement() {
    var uuid = AJS.$('#run-start-date-picker').attr("data-aui-dp-uuid");
    var popupEl = AJS.$('.hasDatepicker[data-aui-dp-popup-uuid="' + uuid + '"]');
    return popupEl;
}

function closeDatePickerDialog() {
    var popupEl = getDatepicketPopupElement();
    var datepickerDialogEl = popupEl.parent().parent();
    datepickerDialogEl.css("display", "none");

    $('#run-start-date-picker').removeClass("active");
}

function clearDatePickerSelection() {
    //remove dialog from dom
    var popupEl = getDatepicketPopupElement();
    var datepickerDialogEl = popupEl.parent().parent().remove();

    //remove data-aui-dp-uuid and value from input element
    $('#run-start-date-picker').removeAttr("value");
    $('#run-start-date-picker').removeAttr("data-aui-dp-uuid");
}

function showRunStartDatePicker() {
    initDatePicker();
    $("#run-start-date-picker").focus();
    setTimeout(function () {
        addButtonsToDatePicker();
    }, 100);
}

