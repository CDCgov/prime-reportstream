  (async () => {
    const reports = await fetchReports();
    reports.forEach( report => {
     document.getElementById( "tBody").innerHTML +=
              `<tr>
                <th data-title="reportId" scope="row"><a href="/report-details/?${report.reportId}" class="usa-link">${report.reportId}</a></th>
                <th data-title="date" scope="row">${ moment.utc( report.sent ).local().format( 'YYYY-MM-DD HH:mm') }</th>
                <th date-title="expires" scope="row">${moment.utc( report.expires ).local().format( 'YYYY-MM-DD HH:mm') }</th>
                <th data-title="Total tests" scope="row">${report.total}</th>
                <th data-title="File" scope="row"><span><a href="#" onclick="requestFile( \'${report.reportId}\');event.preventDefault();">${report.fileType == "HL7_BATCH"? "HL7(BATCH)" : report.fileType }</a></span>
                </th>
              </tr>`
    })
  })();
