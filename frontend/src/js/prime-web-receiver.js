/**
 * Fetch all information for the display of the cards
 * 
 * @returns Array of card objects for the cardGrid
 */
async function fetchCards() {
    const config = { headers: { 'Authorization': `Bearer ${window.jwt}` } };
    const baseURL = getBaseUrl();

    return Promise.all([
        axios.get(`${baseURL}/api/history/summary/tests`, config).then(res => res.data)
    ]);
}

/**
 * 
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
 * 
 */
function checkJWT() {
    const token = window.sessionStorage.getItem("jwt");

    const claims = token ? JSON.parse(atob(token.split('.')[1])) : null;

    if (!token || !claims || moment().isAfter(moment.unix(claims.exp)))
        window.location.replace('/sign-in/?return=/daily-data/');
    else {
        const signIn = document.getElementById("signIn");
        if (signIn) signIn.style.display = "block";
        window.org = claims.organization[0];
        window.user = claims.sub;
        window.jwt = token;
    }
}

/**
 * 
 * @returns 
 */
async function fetchOrgName() {
    const config = { headers: { 'Authorization': `Bearer ${window.jwt}` } }
    const baseURL = getBaseUrl();
    if (window.org == undefined) return null;
 
    return  Promise.all([
        axios.get(`${baseURL}/api/settings/organizations/${window.org.substring(2).replaceAll("_", "-")}`, config)
            .then(res => res.data)
            .then(org => org.description)
    ]);
}


/**
 * 
 */
function idleTimer() {
    const loggedIn = window.sessionStorage.getItem("jwt");

    if (loggedIn) {
        window.sessionStorage.setItem("idle-timer", "true");
        idleTimeout(() => {
            window.sessionStorage.clear()
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
 * 
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
 * 
 */
function logout() {
    window.sessionStorage.removeItem("jwt")
    window.sessionStorage.removeItem("idle-timer")
    window.location.replace(`${window.location.origin}`);
    const _signIn = document.getElementById("signIn");
    if (_signIn) _signIn.style.display = "block";
}

/**
 * 
 */
async function fetchReports() {
    const config = { headers: { 'Authorization': `Bearer ${window.jwt}` } };
    const baseURL = getBaseUrl();

    return axios.get(`${baseURL}/api/history/report`, config).then(res => res.data);
}

/**
 * 
 */
function requestFile(reportId) {
    let baseURL = getBaseUrl();
    let config = { headers: { 'Authorization': `Bearer ${window.jwt}` } }

    return axios.get(`${baseURL}/api/history/report/${reportId}`, config)
        .then(res => res.data)
        .then(csv => download(csv.content, csv.filename, csv.mimetype))
}

/**
 * 
 */
function signIn() {
    const _signIn = document.getElementById("signIn");
    let navMenu = document.getElementById("navmenu");
    if (window.sessionStorage.getItem("jwt")) {
        if (_signIn) _signIn.style.display = "none";
    } else {
        if (_signIn) _signIn.style.display = "block";
        if (navMenu) navMenu.style.display = "none";
    }
}

/**
 * 
 * @returns 
 */
function getBaseUrl() {
    if( window.location.origin.includes("localhost") ) return "http://localhost:7071";
    else if ( window.location.origin.includes("staging") ) return "https://staging.prime.cdc.gov";
    else "https://prime.cdc.gov";
}

(async () => {

    // checkJWT - no redirect
    // user/logout
    let token = window.sessionStorage.getItem("jwt");
    let claims = token ? JSON.parse(atob(token.split('.')[1])) : null;

    if (token && claims && moment().isBefore(moment.unix(claims.exp))) {
        const emailUser = document.getElementById("emailUser");
        if (emailUser) emailUser.innerHTML = claims.sub;
        const logout = document.getElementById("logout");
        if (logout) logout.innerHTML = 'Logout';
        const _signIn = document.getElementById("signIn");
        if (_signIn) _signIn.style.display = "block";
        window.org = claims.organization[0];
        window.user = claims.sub;
        window.jwt = token;
    }

    // orgName
    const orgName = await fetchOrgName();

    if (orgName != null) {
        const orgNameHtml = document.getElementById("orgName");
        if (orgNameHtml) orgNameHtml.innerHTML += orgName;

        // reports
        const reports = await fetchReports();
        reports.forEach( _report => {
            const tBody = document.getElementById("tBody");
            if (tBody) tBody.innerHTML +=
                `<tr>
                <th data-title="reportId" scope="row"><a href="/report-details/?${_report.reportId}" class="usa-link">${_report.reportId}</a></th>
                <th data-title="date" scope="row">${moment.utc(_report.sent).local().format('YYYY-MM-DD HH:mm')}</th>
                <th date-title="expires" scope="row">${moment.utc(_report.expires).local().format('YYYY-MM-DD HH:mm')}</th>
                <th data-title="Total tests" scope="row">${_report.total}</th>
                <th data-title="File" scope="row"><span><a href="#" onclick="requestFile( \'${_report.reportId}\');event.preventDefault();">${_report.fileType == "HL7_BATCH" ? "HL7(BATCH)" : _report.fileType}</a></span>
                </th>
              </tr>`;
        });

        // report
        let report = null;
        if (reports && reports.length > 0){
            if ( window.location.search == "" ) report = reports[0];
            else report = reports.find(report => report.reportId == window.location.search.substring(1));
        } 
        if (report != null){
            const details = document.getElementById("details");
            if (details) details.innerHTML +=
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
                    </div>`;
            const reportId = document.getElementById("report.id");
            if (reportId) reportId.innerHTML = report.reportId;
            const download = document.getElementById("download");
            if (download) download.innerHTML +=
                `<a id="report.fileType" class="usa-button usa-button--outline float-right" href="#" onclick="requestFile( \'${report.reportId}\');event.preventDefault();"></a>`;
            const reportFileType = document.getElementById("report.fileType");
            if (reportFileType) reportFileType.innerHTML = (report.fileType == "HL7" || report.fileType == "HL7_BATCH") ? "HL7" : "CSV";
        }
        // charts
        /*
        const cards = await fetchCards();
        cards.forEach(card => {
            const cards = document.getElementById("cards");
            if (cards) cards.innerHTML +=
                `<div class="tablet:grid-col-6">
            <div class="usa-card__container">
              <div class="usa-card__body">
                <h4 class="text-gray-30 margin-bottom-0">${card.title}</h4>
                <h3 class="text-bold margin-top-0">${card.subtitle}</h3>
                <h4 class="text-gray-30 margin-bottom-0">Last 24 hours</h4>
                <p class="text-bold margin-top-0">${card.daily} &nbsp; &nbsp; &nbsp; <span class="text-heavy ${card.positive ? "text-green" : "text-red"}">
                    ${card.positive ? "&#8599;" : "&#8600;"} ${card.change.toFixed(2)}
                  </span></p>
                <h4 class="text-gray-30 margin-bottom-0">Last 7 days (average)</h4>
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
        }

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
        */
    }
})();