import { Helmet } from "react-helmet";
import { NavLink } from "react-router-dom";
import DOMPurify from "dompurify";

import site from "../../content/site.json";

export const WebReceiverGuide = () => {
    return (
        <>
            <Helmet>
                <title>
                    Data download website guide | How it works |{" "}
                    {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <section id="anchor-top">
                <h1 className="margin-top-0">Data download website guide</h1>
                <p className="usa-intro">
                    The{" "}
                    <NavLink to="/login" key="login" className="usa-link">
                        data download website
                    </NavLink>{" "}
                    enables a public health department to access data from
                    senders via a secure, online portal.
                </p>
                <a
                    href={
                        "mailto:" +
                        DOMPurify.sanitize(site.orgs.RS.email) +
                        "?subject=Getting started with ReportStream"
                    }
                    className="usa-button usa-button--outline"
                >
                    Contact us
                </a>
            </section>
            <hr className="margin-y-6" />
            <section>
                <h2>Benefits of the Web Receiver Experience</h2>
                <ul className="padding-bottom-1">
                    <li>
                        <h3>Receive data from SimpleReport</h3>
                        <p>
                            If your jurisdiction is interested in receiving
                            point-of-care tests results from SimpleReport, you
                            can access data via your ReportStream account.
                        </p>
                    </li>
                    <li>
                        <h3>Get started without ELR</h3>
                        <p>
                            Download data in standard CSV and HL7 formats
                            without setting up an ELR feed.
                        </p>
                    </li>
                    <li>
                        <h3>Secure and personalized</h3>
                        <p>
                            Set up secure, individual logins for each member of
                            your team.
                        </p>
                    </li>
                </ul>
            </section>
            <hr className="margin-top-3 margin-bottom-6" />
            <section>
                <h2>Using the Web Receiver</h2>
                <h3>Recommended browser</h3>

                <p>
                    Please use a modern desktop web browser (ex:{" "}
                    <a
                        href="https://www.google.com/chrome/"
                        target="_blank"
                        rel="noreferrer"
                    >
                        Chrome
                    </a>
                    ,{" "}
                    <a
                        href="https://www.mozilla.org/en-US/firefox/new/"
                        target="_blank"
                        rel="noreferrer"
                    >
                        Firefox
                    </a>
                    ,{" "}
                    <a
                        href="https://www.apple.com/safari/"
                        target="_blank"
                        rel="noreferrer"
                    >
                        Safari
                    </a>
                    ,{" "}
                    <a
                        href="https://www.microsoft.com/en-us/edge"
                        target="_blank"
                        rel="noreferrer"
                    >
                        Edge
                    </a>
                    ) to access the Web Receiver site. Please note: the
                    application does not support Internet Explorer 11 or below.
                </p>

                <h3>General usage</h3>

                <h4>Account creation</h4>
                <p>
                    ReportStream is utilizing a{" "}
                    <a href="https://www.hhs.gov/" className="usa-link">
                        Health and Human Services
                    </a>
                    -owned <a href="https://okta.com">Okta</a> account for
                    managing access to the application. Okta is a U.S. based
                    cloud software provider that specializes in access and
                    identity management.
                </p>
                <ul>
                    <li>
                        You will receive an email from Okta prompting you to
                        sign up.
                    </li>
                    <li>
                        Follow the link in the email to set up your account.
                    </li>
                    <li>Choose a strong password.</li>
                    <li>
                        Choose a two factor authentication method.
                        <ul className="margin-top-1">
                            <li>
                                For increased security, two-factor
                                authentication is required.
                            </li>
                            <li>
                                You can use either SMS or Google Authenticator (
                                <a
                                    href="https://apps.apple.com/us/app/google-authenticator/id388497605"
                                    className="usa-link"
                                >
                                    App Store
                                </a>
                                ,{" "}
                                <a
                                    href="https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2&hl=en_US&gl=US"
                                    className="usa-link"
                                >
                                    Google Play
                                </a>
                                ).
                            </li>
                        </ul>
                    </li>
                </ul>

                <h4>Sign in to the application</h4>

                <ul>
                    <li>
                        Visit{" "}
                        <NavLink to="/login" key="login" className="usa-link">
                            reportstream.cdc.gov
                        </NavLink>{" "}
                        to log in to the application.
                    </li>
                    <li>
                        If you are directed to an internal Okta page and not the
                        data download site, don’t worry! Visit{" "}
                        <NavLink
                            to="/daily-data"
                            key="daily"
                            className="usa-link"
                        >
                            reportstream.cdc.gov/daily-data
                        </NavLink>{" "}
                        to view your data.
                    </li>
                </ul>

                <h4>Account management</h4>
                <p>
                    ReportStream will manually manage user accounts for your
                    team. To add or remove team members, email us at{" "}
                    <a href="mailto:reportstream@cdc.gov" className="usa-link">
                        reportstream@cdc.gov
                    </a>
                    .
                </p>

                <h4>Password reset</h4>
                <ul>
                    <li>
                        If you forgot your password, follow the instructions
                        under "Need help signing in?" on the login page at{" "}
                        <NavLink to="/login" key="login" className="usa-link">
                            reportsream.cdc.gov/login
                        </NavLink>
                        .
                    </li>
                    <li>
                        If you want to update your password, log out of the
                        application and use the password reset process outlined
                        above.
                    </li>
                </ul>

                <h4>Accessing data</h4>
                <p>
                    You will be able to download the most recently reported test
                    result data, as well as up to 30 days of previously reported
                    data. Due to the presence of personally identifiable
                    information or personal health information, ReportStream
                    will not be a permanent repository for reported test data.
                </p>
                <p>
                    Each report will be held for 30 days, and will be accessible
                    through the application for the duration of that period.
                    After 30 days has passed for an individual report, the file
                    will be permanently deleted and will not be recoverable.
                </p>
                <p>
                    Please download data and ingest it into your systems as soon
                    as possible.
                </p>

                <h4>Support</h4>
                <p>
                    Do you have questions, problems, or bugs to report? Email
                    the team at
                    <a
                        href={
                            "mailto:" +
                            DOMPurify.sanitize(site.orgs.RS.email) +
                            "?subject=Getting started with ReportStream"
                        }
                        className="margin-left-1 margin-right-1 usa-link"
                    >
                        reportstream@cdc.gov
                    </a>
                    for help.
                </p>

                <p className="margin-top-8">
                    <a href="#anchor-top" className="usa-link">
                        Back to top
                    </a>
                </p>
            </section>
        </>
    );
};
