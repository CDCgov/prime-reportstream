(async () => {
  const reports = await fetchReports();
  var report = window.location.search == "" ? reports[0] : reports.find(report => report.reportId == window.location.search.substring(1));
  document.getElementById("details").innerHTML +=
    `<div class="tablet:grid-col-6">
                    <h4 class="text-gray-30 margin-bottom-0">Report type</h4>
                    <p class="text-bold margin-top-0">${report.type}</p>
                    <h4 class="text-gray-30 margin-bottom-0">Report sent</h4>
                    <p class="text-bold margin-top-0">${moment.utc(report.sent).local().format('dddd, MMM DD, YYYY  HH:mm')}</p>
            </div>
            <div class="tablet:grid-col-6">
                    <h4 class="text-gray-30 margin-bottom-0">Total reporting</h4>
                    <p class="text-bold margin-top-0">${report.total}</p>
                    <h4 class="text-gray-30 margin-bottom-0">Expires</h4>
                    <p class="text-bold margin-top-0">${moment.utc(report.expires).local().format('dddd, MMM DD, YYYY  HH:mm')}</p>
            </div>`
  document.getElementById("report.id").innerHTML = report.reportId;
  document.getElementById("download").innerHTML +=
    `<a id="report.fileType" class="usa-button usa-button--outline float-right" href="#" onclick="requestFile( \'${report.reportId}\');event.preventDefault();"></a>`
  document.getElementById("report.fileType").innerHTML = (report.fileType == "HL7" || report.fileType == "HL7_BATCH") ? "HL7" : "CSV"

})();