window.GOVUKFrontend.initAll();
window.HMRCFrontend.initAll();

const timeoutDialog = document.querySelector("#timeout-dialog");

if (timeoutDialog &&
    window.HMRCFrontend.TimeoutDialog &&
    window.location.pathname !== "/pay-vat-deferred-due-to-coronavirus/time-out") {
    new window.HMRCFrontend.TimeoutDialog(timeoutDialog).init();
}