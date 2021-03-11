window.GOVUKFrontend.initAll();
window.HMRCFrontend.initAll();

const timeoutDialog = document.querySelector("#timeout-dialog");

if (timeoutDialog &&
    window.HMRCFrontend.TimeoutDialog &&
    window.location.pathname !== "/pay-vat-deferred-due-to-coronavirus/time-out") {
    new window.HMRCFrontend.TimeoutDialog(timeoutDialog).init();
}

function printElement(elId, ms) {
    var divToPrint = document.getElementById(elId);
    newWin = window.open();
    newWin.document.write(divToPrint.innerHTML);
    newWin.focus();
    setTimeout(function() {printNewWindow()}, ms);
}
function printNewWindow(){
    newWin.print();
    newWin.close();
};

function directDebitSubmit() {
    document.addEventListener('DOMContentLoaded', function() {
        const ddSubmitButton = document.getElementById('submit-dd')
        if(typeof(ddSubmitButton) != 'undefined' && ddSubmitButton != null){
            document.getElementById('submit-dd-form').addEventListener('submit', function(e) {
                ddSubmitButton.setAttribute('disabled', true)
            })
        }
    })
}
directDebitSubmit()
