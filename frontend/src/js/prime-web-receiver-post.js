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
        console.log(`processing Reports ${feed} ${idx}`)
        await processReports( feed, idx );
    })

    // charts
    /* processCharts(); */
    console.log("fresh!");
})();