/** Convert Org name
 * from DHzz_phd
 * to zz-phd
 * which is the format the ReportStream API endpoints are expecting
 * @returns {*|string}
 */
function convertOrgName(claimsOrgName) {
    return claimsOrgName.substring(2).replaceAll("_", "-");
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
function apiConfig() {
    return {
        headers: {
            'Authorization': `Bearer ${window.jwt}`,
            'Organization': getClaimsOrgValue()
        }
    };
}

/**
 * Fetch all information for the display of the cards
 *
 * @returns Array of card objects for the cardGrid
 */
async function fetchCards() {
    const baseURL = getBaseUrl();

    return window.jwt? Promise.all([
        axios.get(`${baseURL}/api/history/summary/tests`, apiConfig()).then(res => res.data)
    ]) : [];
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
 * Validates the JWT token as stored in session storage under the key "jwt";
 *  if not valid, redirects to the sign-in page; otherwise sets up the claims
 */
function checkJWT() {
    const token = window.sessionStorage.getItem("jwt");
    const claims = token ? JSON.parse(atob(token.split('.')[1])) : null;

    if (!token || !claims || moment().isAfter(moment.unix(claims.exp)))
        window.location.replace('/sign-in/?return=/daily-data/');
}

/**
 * Fetches the organization name based on the stored claim of org
 *
 * @returns Promise, eventually a String organization name
 */
async function fetchOrgName() {

    if (!window.org || !window.jwt) return null;
    const baseURL = getBaseUrl();

    const orgName = convertOrgName(window.org);

    return isLocalhost()? "Localhost Public Health Department" : Promise.all([
        axios.get(`${baseURL}/api/settings/organizations/${window.org.substring(2).replaceAll("_", "-")}`, config)
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
    const loggedIn = window.sessionStorage.getItem("jwt");

    if (loggedIn) {
        window.sessionStorage.setItem("idle-timer", "true");
        idleTimeout(() => {
            window.sessionStorage.clear();
            window.location.replace(`/sign-in/`);
        },
            {
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

    new OktaSignIn(config).showSignInToGetTokens({ scopes: ['openid', 'email', 'profile'] })
        .then(function (tokens) {
            var jwt = tokens.accessToken.value;
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

/**
 *
 */
async function fetchReports() {
    const baseURL = getBaseUrl();
    return isLocalhost()? ReportData : window.jwt? axios.get(`${baseURL}/api/history/report`, config).then(res => res.data) : [];
}

/**
 *
 */
function requestFile(reportId) {
    let baseURL = getBaseUrl();

    return window.jwt? axios.get(`${baseURL}/api/history/report/${reportId}`, apiConfig())
        .then(res => res.data)
        .then(csv => download(csv.content, csv.filename, csv.mimetype)) : null;
}

/**
 *

function signIn() {
    const _signIn = document.getElementById("signIn");
    let _navMenu = document.getElementById("navmenu");
    if (window.sessionStorage.getItem("jwt")) {
        if (_signIn){ _signIn.style.display = "none"; console.log( 'setting signIn to none' ); }

    } else {
        if (_signIn){ _signIn.style.display = "block"; console.log( 'setting signIn to block' ); }
        if (_navMenu){ _navMenu.style.display = "none"; console.log( 'setting navMenu to none' ); }
    }
}
*/

/**
 * Determines if the system is running as localhost
 * 
 * @returns 
 */
function isLocalhost(){
    return window.location.origin.includes("localhost:8088");    
}

/**
 *
 * @returns
 */
function getBaseUrl() {
    if (window.location.origin.includes("localhost"))
        return "http://localhost:7071";
    else if (window.location.origin.includes("staging"))
        return "https://staging.prime.cdc.gov";
    else
        return "https://prime.cdc.gov";
}


function changeOrg( event ){
    window.org = event.value;
    window.sessionStorage.setItem( "oldOrg", window.org );
    processOrgName();
    processReports();
}
/**
 *
 * @param {boolean} redirect
 */
function processJwtToken(){
    let token = window.sessionStorage.getItem("jwt");
    let claims = token ? JSON.parse(atob(token.split('.')[1])) : null;

    if (token && claims && moment().isBefore(moment.unix(claims.exp))) {

        const emailUser = document.getElementById("emailUser");
        if (emailUser) emailUser.innerHTML = claims.sub;
        const logout = document.getElementById("logout");
        if (logout) logout.innerHTML = 'Logout';

        const _signIn = document.getElementById("signInButton");
        if (_signIn) { _signIn.setAttribute( "hidden", "hidden"); }

        const _org = claims.organization.filter(c => c !== "DHPrimeAdmins");

        const oldOrg = window.sessionStorage.getItem( "oldOrg");

        window.org = oldOrg? oldOrg : (_org && _org.length > 0) ? _org[0] : null;

        window.sessionStorage.setItem( "oldOrg", window.org );
        window.orgs = _org;
        window.user = claims.sub;
        window.jwt = token;

        if (claims.organization.includes( "DHPrimeAdmins" ) ) {
            const dropDownWrapper = document.getElementById("orgDropdown");
            dropDownWrapper.classList.remove("display-none");
            dropDownWrapper.classList.add("display-block");
            const _dropdown = document.createElement("div");
            _dropdown.id = "dropdown";

            let _orgsOptions = "";

            window.orgs.forEach( org => {
                _orgsOptions +=
                    `
                        <option value="${org}" ${window.org === org ? 'selected="selected"' : ""} >
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
    else{
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

/**
 *
 * @returns {Array<Report>} an array of the received reports; possibly empty
 */
async function processReports(){
    let reports = [];
    const tBody = document.getElementById("tBody");
    // clear the table body because we can get reports from different PHDs
    if (tBody) tBody.innerHTML = "";
    try {
        reports = await fetchReports();
    } catch (error) {
        console.log('fetchReports() is failing');
        console.error(error);
    }
    reports.forEach(_report => {
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
    return reports;
}

/**
 *
 * @param {Array<Report>} reports array
 * @returns {Report} selected report; possibly null
 */
async function processReport( reports ){
    let report = null;
    if (reports && reports.length > 0) {
        if (window.location.search == "") report = reports[0];
        else report = reports.find(report => report.reportId == window.location.search.substring(1));
    }
    if (report != null) {
        const details = document.getElementById("details");
        if (details) details.innerHTML +=
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
        const facilities = document.getElementById( "tBodyFac");
        if( facilities ){
            report.facilities.forEach( reportFacility => {
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
       
        const noFac = document.getElementById( 'nofacilities' );
        const facTable = document.getElementById( 'facilitiestable');


        if( report.facilities.length ){
            if( noFac ) noFac.setAttribute( "hidden", "hidden" );    
        }
        else{
            if( facTable ) facTable.setAttribute( "hidden", "hidden" );
        }

        const reportId = document.getElementById("report.id");
        if (reportId) reportId.innerHTML = report.reportId;
        const download = document.getElementById("download");
        if (download) download.innerHTML +=
            `<a id="report.fileType"
                class="usa-button usa-button--outline float-right"
                href="javascript:requestFile( \'${report.reportId}\');"</a>`;
        const reportFileType = document.getElementById("report.fileType");
        if (reportFileType) reportFileType.innerHTML = (report.fileType == "HL7" || report.fileType == "HL7_BATCH") ? "HL7" : "CSV";
    }
    return report;
}

/**
 *
 */
async function processCharts(){
    let cards = [];
    try{
        cards = await fetchCards();
    } catch( error ){
        console.log( 'fetchCards() is failing' );
        console.error( error );
    }
    cards.forEach(card => {
        const cards = document.getElementById("cards");
        if (cards) cards.innerHTML +=
        `<div class="tablet:grid-col-6">
            <div class="usa-card__container">
            <div class="usa-card__body">
                <h4 class="text-base margin-bottom-0">${card.title}</h4>
                <h3 class="text-bold margin-top-0">${card.subtitle}</h3>
                <h4 class="text-base margin-bottom-0">Last 24 hours</h4>
                <p class="text-bold margin-top-0">${card.daily} &nbsp; &nbsp; &nbsp; <span class="text-heavy ${card.positive ? "text-green" : "text-red"}">
                    ${card.positive ? "&#8599;" : "&#8600;"} ${card.change.toFixed(2)}
                </span></p>
                <h4 class="text-base margin-bottom-0">Last 7 days (average)</h4>
                <p class="text-bold margin-top-0">${card.last}</p>
                <canvas id="${card.id}" width="200" height="40"></canvas>
            </div>
            </div>
        </div>`;
    });
    var ctx = 'summary-tests';
    var options = {
        plugins: {
            legend: {
                display: false,
            }
        },
        scales: {
            y: {
                ticks: {
                    beginAtZero: false
                },
                display: false,
            },
            x: {
                display: false,
            }
        }
    };

    var labels = [];
    for (var i = 7; i >= 0; i--) {
        labels.push(moment().subtract(i, 'days').format("YYYY-MM-DD"))
    }

    const myLineChartHtml = document.getElementById(ctx);
    if (myLineChartHtml) {
        new Chart(ctx, {
            type: 'line',
            data: {

                labels: labels,
                datasets: [{
                    data: cards[0].data,
                    borderColor: "#4682B4",
                    backgroundColor: "#B0C4DE",
                    fill: 'origin',
                    borderJoinStyle: "round"
                }]
            },
            options: options
        });
    }

}
