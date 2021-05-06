/**
 * 
 */
 (async () => {

    // checkJWT - no redirect
    processJwtToken();

    // orgName
    let orgName = await processOrgName();

    // reports
    let reports = await processReports();

    // report
    let report = await processReport( reports );

    // charts
    /* processCharts(); */
})();