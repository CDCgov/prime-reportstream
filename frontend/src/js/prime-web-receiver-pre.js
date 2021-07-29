const debug = (message) => {
    // get the URL
    const params = (new URL(document.location)).searchParams;
    // if the debug value exists
    if (params.has("debug")) {
        // log the message
        console.log(message);
    }
};

/** Convert Org name
 * from DHzz_phd
 * to zz-phd
 * which is the format the ReportStream API endpoints are expecting
 * @returns {*|string}
 */
function convertOrgName(claimsOrgName) {
    return claimsOrgName.indexOf("DH") === 0 ?
        claimsOrgName.substring(2).replaceAll("_", "-") :
        claimsOrgName;
}

/**
 * gets the base url
 * @returns {string}
 */
function getBaseUrl() {
    if (window.location.origin.includes("localhost"))
        return "http://localhost:7071";
    else if (window.location.origin.includes("staging"))
        return "https://staging.prime.cdc.gov";
    else
        return "https://prime.cdc.gov";
}

/** getClaimsOrgValue
 * ensures a string is returned for window.org
 * @returns {*|string}
 */
function getClaimsOrgValue() {
    return window.org ? window.org : "";
}

/** apiConfig
 * used for axios headers to call ReportStream api endpoints
 * @returns {{headers: {Authorization: string, Organization: (*|string)}}}
 */
function apiConfig(url) {
    debug(`apiConfig organization = ${getClaimsOrgValue()}`);

    return {
        url: url,
        baseURL: `${getBaseUrl()}/api/`,
        headers: {
            'Authorization': `Bearer ${window.jwt}`,
            'Organization': getClaimsOrgValue()
        }
    };
}

/**
 * Checks that the browser is on the accepted list of browsers; if not redirects
 *  to a "unsupported-browser" page
 */
function checkBrowser() {
    const browser = bowser.getParser(window.navigator.userAgent);
    const isValidBrowser = browser.satisfies({
        "edge": ">86.0",
        "chrome": ">86.0",
        "firefox": ">78.0",
        "safari": ">14"
    });

    if (!isValidBrowser) window.location.replace('unsupported-browser.html');
}

/**
 * Determines if a user is logged in
 * 
 * @returns truthy if logged in; falsy otherwise
 */
function isLoggedIn(){
    const token = window.sessionStorage.getItem("jwt");
    const claims = token ? JSON.parse(atob(token.split('.')[1])) : null;
    return (token && claims && moment().isBefore(moment.unix(claims.exp)))
}

/**
 * Validates the JWT token as stored in session storage under the key "jwt";
 *  if not valid, redirects to the sign-in page; otherwise sets up the claims
 */
function checkJWT() {
    if (!isLoggedIn())
        window.location.replace('/sign-in/?return=/daily-data/');
}

/**
 * Fetches the organization name based on the stored claim of org
 *
 * @returns Promise, eventually a String organization name
 */
async function fetchOrgName() {
    if (!window.org || !window.jwt) return null;
    const orgName = convertOrgName(window.org);
    debug(`getting orgName for ${orgName}: ${window.org}`)
    const url = `settings/organizations/${orgName}`;
    debug(url);
    return Promise.all([
        axios(apiConfig(url))
            .then(res => res.data)
            .then(org => org.description)
    ]);
}


/**
 *  If the user is logged in, starts an idle timer that when expires
 *      after 15 min, clears session storage and redirects to the
 *      sign-in page
 */
function idleTimer() {
    if (isLoggedIn()) {
        window.sessionStorage.setItem("idle-timer", "true");
        idleTimeout(() => {
            window.sessionStorage.clear();
            window.location.replace(`/sign-in/`);
        }, {
                element: document,
                timeout: 1000 * 60 * 15,
                loop: false
            });
    }
}

/**
 * Handles OKTA login by displaying and sending to OKTA; once successfully
 *  logged in, directs to /daily-data page
 */
function login() {
    const OKTA_clientId = '0oa6fm8j4G1xfrthd4h6';
    const redirectUri = window.location.origin;

    const config = {
        logo: '//logo.clearbit.com/cdc.gov',
        language: 'en',
        features: {
            registration: false, // Enable self-service registration flow
            rememberMe: false, // Setting to false will remove the checkbox to save username
            router: true, // Leave this set to true for the API demo
        },
        el: "#okta-login-container",
        baseUrl: `https://hhs-prime.okta.com`,
        clientId: `${OKTA_clientId}`,
        redirectUri: redirectUri,
        authParams: {
            issuer: `https://hhs-prime.okta.com/oauth2/default`
        }
    };

    new OktaSignIn(config)
        .showSignInToGetTokens({ scopes: ['openid', 'email', 'profile'] })
        .then(function (tokens) {
            const jwt = tokens.accessToken.value;
            window.sessionStorage.setItem('jwt', jwt);
            window.location.replace(`${window.location.origin}/daily-data/`);
        });
}

/**
 * Logs out the the current user
 */
function logout() {
    window.sessionStorage.removeItem("jwt");
    window.sessionStorage.removeItem("idle-timer");
    window.sessionStorage.removeItem("oldOrg");
    window.location.replace(`${window.location.origin}`);
    const _signIn = document.getElementById("signInButton");
    if (_signIn){
        _signIn.removeAttribute( "hidden" );
    }
}

async function fetchReportFeeds(){
    let reports = await fetchReports();
    let receivingOrgSvc = reports ? reports.map( rep => {
        debug(`display name = ${rep.displayName}`)
        return rep.receivingOrgSvc
    }) : []
    const externalNames = new Set(receivingOrgSvc);
    return [...externalNames];
}

/**
 *
 */
async function fetchReports(filter) {
    let url = `history/report?cache=${new Date().toISOString()}`;
    debug(`invoking ${url}`);
    const retValue = window.jwt ? await axios(apiConfig(url))
        .then(res => res.data)
        .catch(e => { console.log(e); return [] }): [];
    debug(retValue);
    return filter? retValue.filter( report => report.receivingOrgSvc === filter ) : retValue;
}

async function fetchAllOrgs() {
    return window.jwt ? await axios(apiConfig('settings/organizations'))
        .then(res => res.data) : [];
}

/**
 *
 */
async function requestFile(reportId) {
    return window.jwt ? await axios(apiConfig(`history/report/${reportId}`))
        .then(res => res.data)
        .then(csv => {
            // The filename to use for the download should not contain blob folders if present
            let filename = decodeURIComponent(csv.filename);
            let filenameStartIndex = filename.lastIndexOf("/");
            if (filenameStartIndex >= 0 && filename.length > filenameStartIndex + 1) 
                filename = filename.substring(filenameStartIndex + 1);
            download(csv.content, filename, csv.mimetype)
        }) : null;
}

/**
 * Determines if the system is running as localhost
 *
 * @returns
 */
function isLocalhost(){
    return window.location.origin.includes("localhost");
}

/**
 *
 * @returns
 */
async function changeOrg(event){
    debug(`org = ${event.value}`)
    window.org = event.value;
    window.sessionStorage.setItem( "oldOrg", window.org );
    processOrgName();
    let feeds = await processReportFeeds();

    // reports
    // I don't think I'm necessary now
    let promises = feeds.map(async (feed,idx) => {
        debug(`processing Reports ${feed} ${idx}`);
        await processReports(feed, idx);
    });
    Promise.all(promises).then(_results => debug(_results));

    await processReport(await fetchReports());
}

function populateOrgDropdown() {
    // it's possible someone could be a part of more than one sending/receiving org
    if (window.orgs && window.orgs.length > 0) {
        const dropDownWrapper = document.querySelector("#orgDropdown");
        dropDownWrapper.classList.remove("display-none");
        dropDownWrapper.classList.add("display-block");
        const _dropdown = document.createElement("div");
        _dropdown.id = "dropdown";

        let _orgsOptions = "";
        window.orgs.sort().forEach( org => {
            _orgsOptions +=
                `
                        <option value="${org}" ${convertOrgName(window.org) === org ? 'selected="selected"' : ""} >
                            ${convertOrgName(org).toUpperCase()}
                        </option>
                    `;
        });
        _dropdown.innerHTML =
            `
                    <select aria-label="Select Org" class="usa-select" name="orgs" id="orgs" onchange="changeOrg(this)">
                        ${_orgsOptions}
                    </select>
                `;
        dropDownWrapper.prepend(_dropdown);
    }
}

function isAnAdmin() {
    const token = window.sessionStorage.getItem("jwt");
    const claims = token ? JSON.parse(atob(token.split('.')[1])) : null;
    return !!(claims && claims.organization && claims.organization.includes("DHPrimeAdmins"));
}

/**
 *
 * @param {boolean} redirect
 */
function processJwtToken(){
    let token = window.sessionStorage.getItem("jwt");
    let claims = token ? JSON.parse(atob(token.split('.')[1])) : null;

    if (token && claims && moment().isBefore(moment.unix(claims.exp))) {
        // update the email user link
        const emailUser = document.querySelector("#emailUser");
        if (emailUser) emailUser.innerHTML = claims.sub;
        // refresh the logout link
        const logout = document.querySelector("#logout");
        if (logout) logout.innerHTML = 'Logout';
        // refresh our signin link/remove it
        const _signIn = document.querySelector("#signInButton");
        if (_signIn) {
            _signIn.setAttribute( "hidden", "hidden");
        }
        // process the org so the dropdown looks correct
        const _org = claims.organization.filter(c => c !== "DHPrimeAdmins");
        const oldOrg = window.sessionStorage.getItem( "oldOrg");
        window.org = oldOrg ? oldOrg : (_org && _org.length > 0) ? convertOrgName( _org[0] ) : null;
        window.sessionStorage.setItem( "oldOrg", window.org );
        window.user = claims.sub;
        // set the token here
        window.jwt = token;

        if (claims.organization.includes("DHPrimeAdmins")) {
            fetchAllOrgs().then((data) => {
                const allOrgs = data.map((org) => {
                    return org.name;
                });
                window.orgs = allOrgs.sort();
                populateOrgDropdown();
            });
        } else {
            window.orgs = claims.organization.filter(c => c !== "DHPrimeAdmins").sort();
            populateOrgDropdown();
        }
    } else {
        const navmenu = document.getElementById( "navmenu" );
        if( navmenu ) navmenu.setAttribute( "hidden", "hidden" );

        const _signIn = document.getElementById( "signInButton" );
        if( _signIn ) _signIn.removeAttribute( "hidden" );
    }
}

/**
 *
 * @returns {string} organization name; possibly null
 */
async function processOrgName(){
    let orgName = null;

    try {
        orgName = await fetchOrgName();

    } catch (error) {
        console.log('fetchOrgName() is failing');
        console.error(error);
    }

    const orgNameHtml = document.getElementById("orgName");
    if (orgNameHtml && orgName) orgNameHtml.innerHTML = orgName;

    return orgName;
}

function titleCase(str) {
    str = str.toLowerCase().split(' ');
    for (let i = 0; i < str.length; i++) {
      str[i] = str[i].charAt(0).toUpperCase() + str[i].slice(1); 
    }
    return str.join(' ');
  }

async function processReportFeeds(){
    const feeds = await fetchReportFeeds();
    const tabs = document.querySelector("#tabs");
    debug(feeds);
    if (tabs) {
        tabs.innerHTML = "";
        tabs.innerHTML += `<div id="reportFeeds" class=${feeds.length>1?"tab-wrap":""}></div>`
    }
    const reportFeeds = document.querySelector("#reportFeeds");
    if (reportFeeds) {
        reportFeeds.innerHTML = "";
        if (feeds.length === 0) {
            reportFeeds.innerHTML = `
                    <div class="${feeds.length > 1 ? "tab__content" : ""}">
                        <table class="usa-table usa-table--borderless prime-table" summary="Previous results">
                        <thead>
                          <tr>
                            <th scope="col">Report Id</th>
                            <th scope="col">Date Sent</th>
                            <th scope="col">Expires</th>
                            <th scope="col">Total tests</th>
                            <th scope="col">File</th>
                          </tr>
                        </thead>
                        <tbody id="tBody" class="font-mono-2xs">
                            <tr>
                                <th colspan="5">No reports found</th>
                            </tr>
                        </tbody>
                      </table>
                    </div>
                `
            return [];
        }

        if (feeds.length > 1) {
            feeds.forEach((feed, idx) => {
                debug(`building tab${idx}`);
                reportFeeds.innerHTML += `
                    <input type="radio" id="tab${idx}" name="tabGroup1" class="tab" ${idx > 0 ? "" : "checked"} data-feed-name="${feed}">
                    <label for="tab${idx}">${feed.replaceAll("-", " ").toUpperCase()}</label>
                    `
            });
        }
        // loop the feeds and kick out the tables
        feeds.forEach((feed, idx) => {
            reportFeeds.innerHTML += `
                    <div class="${feeds.length > 1 ? "tab__content" : ""}" data-feed-name="${feed}">
                        <table class="usa-table usa-table--borderless prime-table" summary="Previous results">
                        <thead>
                          <tr>
                            <th scope="col">Report Id</th>
                            <th scope="col">Date Sent</th>
                            <th scope="col">Expires</th>
                            <th scope="col">Total tests</th>
                            <th scope="col">File</th>
                          </tr>
                        </thead>
                        <tbody id="tBody${idx ? idx : ''}" class="font-mono-2xs" data-feed-name="${feed}">
                        </tbody>
                      </table>
                    </div>
                `
        });
    }

    return feeds;
}

/**
 *
 * @returns {Array<Report>} an array of the received reports; possibly empty
 */
async function processReports(feed, idx){
    let reports = [];
    const tBody = document.querySelector(`tbody[data-feed-name='${feed}']`);
    debug(tBody);
    // clear the table body because we can get reports from different PHDs
    if (tBody) tBody.innerHTML = "";
    try {
        reports = await fetchReports(feed);
    } catch (error) {
        console.log('fetchReports() is failing');
        console.error(error);
    }
    // verify the reports exist
    if (reports) {
        // if they do then write them out
        reports.forEach(_report => {
            const tBody = document.querySelector(`tBody[data-feed-name='${feed}']`);
            if (tBody) tBody.innerHTML +=
                `<tr>
                    <th data-title="reportId" scope="row">
                        <a href="/report-details/?${_report.reportId}" class="usa-link">${_report.reportId}</a>
                    </th>
                    <th data-title="date" scope="row">${moment.utc(_report.sent).local().format('YYYY-MM-DD HH:mm')}</th>
                    <th date-title="expires" scope="row">${moment.utc(_report.expires).local().format('YYYY-MM-DD HH:mm')}</th>
                    <th data-title="Total tests" scope="row">${_report.total}</th>
                    <th data-title="File" scope="row">
                        <span>
                            <a href="javascript:requestFile( \'${_report.reportId}\');" class="usa-link">
                                ${_report.fileType == "HL7_BATCH" ? "HL7(BATCH)" : _report.fileType}
                            </a>
                        </span>
                    </th>
              </tr>`;
    });
    }
    if (!reports || reports.length === 0) {
        if (tBody) tBody.innerHTML +=
            `<tr>
                <th colspan="5">No reports found</th>
            </tr>`;
    }
    return reports;
}

const sortFacilities = (facilityOne, facilityTwo) => {
  const fOne = facilityOne.facility.toUpperCase();
  const fTwo = facilityTwo.facility.toUpperCase();

  if (fOne < fTwo) {
      return -1;
  }
  if (fOne > fTwo) {
      return 1;
  }
  return 0;
};

/**
 *
 * @param {Array<Report>} reports array
 * @returns {Report} selected report; possibly null
 */
async function processReport( reports ){
    let report = null;
    if (reports && reports.length > 0) {
        if (window.location.search === "")
            report = reports[0];
        else
            report = reports.find(report => report.reportId === window.location.search.substring(1));
    }
    if (report) {
        const details = document.getElementById("details");
        if (details) {
            details.innerHTML +=
                `<div class="tablet:grid-col-6">
                            <h4 class="text-base-darker text-normal margin-bottom-0">Report type</h4>
                            <p class="text-bold margin-top-0">${report.type}</p>
                            <h4 class="text-base-darker text-normal margin-bottom-0">Report sent</h4>
                            <p class="text-bold margin-top-0">${moment.utc(report.sent).local().format('dddd, MMM DD, YYYY  HH:mm')}</p>
                    </div>
                    <div class="tablet:grid-col-6">
                            <h4 class="text-base-darker text-normal margin-bottom-0">Total tests reported</h4>
                            <p class="text-bold margin-top-0">${report.total}</p>
                            <h4 class="text-base-darker text-normal margin-bottom-0">Download expires</h4>
                            <p class="text-bold margin-top-0">${moment.utc(report.expires).local().format('dddd, MMM DD, YYYY  HH:mm')}</p>
                    </div>`;
        }
        const facilities = document.querySelector( "#tBodyFac");
        if (facilities && report.facilities) {
            report.facilities.sort(sortFacilities).forEach(reportFacility => {
                facilities.innerHTML +=
                    `
                    <tr>
                        <td>${reportFacility.facility}</td>
                        <td>${reportFacility.CLIA}</td>
                        <td>${reportFacility.total}</td>
                    </tr>
                    `;
            });
        }

        const noFac = document.querySelector( '#nofacilities' );
        const facTable = document.querySelector( '#facilitiestable');

        if (report.facilities && report.facilities.length) {
            if (noFac) noFac.setAttribute( "hidden", "hidden" );
        } else {
            if (facTable) facTable.setAttribute( "hidden", "hidden" );
        }

        const reportId = document.querySelector("#report.id");
        if (reportId) reportId.innerHTML = report.reportId;
        const download = document.querySelector("#download");
        if (download) {
            download.innerHTML +=
                `<a id="report.fileType"
                class="usa-button usa-button--outline float-right"
                href="javascript:requestFile( \'${report.reportId}\');"</a>`;
        }
        const reportFileType = document.getElementById("report.fileType");
        if (reportFileType)
            reportFileType.innerHTML = (report.fileType == "HL7" || report.fileType == "HL7_BATCH") ? "HL7" : "CSV";
    }

    return report;
}

