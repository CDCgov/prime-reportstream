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
    const feeds = await processReportFeeds();

    // reports
    const promises = feeds.map(async (feed,idx) => {
        console.log(`processing Reports ${feed} ${idx}`);
        await processReports( feed, idx );
    });
    Promise.all(promises).then(_result => console.log("Done!"));

    await processReport(await fetchReports());

    // charts
    /* processCharts(); */
})().catch(err => {
    console.error(err);
});
