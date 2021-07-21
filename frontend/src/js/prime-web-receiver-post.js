/**
 * 
 */
 (async () => {

    // checkJWT - no redirect
    processJwtToken();

    // start the idle timer
    idleTimer();

    // orgName
    let orgName = await processOrgName();

    console.log( 'process report feeds' );
    let feeds = await processReportFeeds();

    // reports
    feeds.forEach( async (feed,idx) => {
        await processReports( feed, idx );
    })
    let reports = await processReports();

    

    // report
    let report = await processReport( await fetchReports() );

    // charts
    /* processCharts(); */
    console.log("fresh!");
})();