import { NavLink } from "react-router-dom";

import { BasicHelmet } from "../../../components/header/BasicHelmet";
import { ResourcesDirectories } from "../../../content/resources";

export const DataDownloadGuideIa = () => {
    return (
        <>
            <BasicHelmet
                pageTitle={`${ResourcesDirectories.DOWNLOAD_GUIDE} | Resources`}
            />
            <h1 id="anchor-top">{ResourcesDirectories.DOWNLOAD_GUIDE}</h1>
            <h2>
                Instructions for public health departments to download data
                manually from the ReportStream application.
            </h2>

            <section>
                <div className="usa-alert usa-alert--info margin-y-6 measure-6">
                    <div className="usa-alert__body">
                        <h3 className="usa-alert__heading font-body-md margin-top-05">
                            Why download data manually from ReportStream?
                        </h3>
                        <p>
                            <strong>Get started w/o an ELR connection</strong>:
                            Download data in standard CSV and HL7 formats
                            without setting up an ELR feed.
                        </p>
                        <p>
                            <strong>Access back-up data</strong>: Get your
                            jurisdiction data manually in the case of a
                            temporary connection failure.
                        </p>
                        <p>
                            <strong>Secure and personalized </strong>: Set up
                            secure, individual logins for each member of your
                            team.
                        </p>
                    </div>
                </div>
            </section>
            <section>
                <h2>Download data from ReportStream</h2>

                <h4>General usage</h4>

                <h3>Create an account</h3>
                <p>
                    To gain access to the ReportStream application, follow the
                    steps outlined in the{" "}
                    <a href="/resources/account-registration-guide">
                        Account Registration Guide
                    </a>
                    .
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
                <h4>Recommended browser</h4>

                <p>
                    Please use a modern desktop web browser (ex:{" "}
                    <a
                        href="https://www.google.com/chrome/"
                        target="_blank"
                        rel="noreferrer"
                        className="usa-link"
                    >
                        Chrome
                    </a>
                    ,{" "}
                    <a
                        href="https://www.mozilla.org/en-US/firefox/new/"
                        target="_blank"
                        rel="noreferrer"
                        className="usa-link"
                    >
                        Firefox
                    </a>
                    ,{" "}
                    <a
                        href="https://www.apple.com/safari/"
                        target="_blank"
                        rel="noreferrer"
                        className="usa-link"
                    >
                        Safari
                    </a>
                    ,{" "}
                    <a
                        href="https://www.microsoft.com/en-us/edge"
                        target="_blank"
                        rel="noreferrer"
                        className="usa-link"
                    >
                        Edge
                    </a>
                    ) to access the site.
                </p>
                <p>
                    <em>
                        Please note: the application does not support Internet
                        Explorer 11 or below.
                    </em>
                </p>

                <h3>Log in to ReportStream</h3>

                <ul>
                    <li>
                        Visit{" "}
                        <NavLink to="/login" key="login" className="usa-link">
                            reportstream.cdc.gov
                        </NavLink>{" "}
                        using a modern desktop web browser (ex: Chrome, Firefox,
                        Safari, Edge) and log in. Please note: ReportStream does
                        not support Internet Explorer 11 or below.
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

                <h3>Access your jurisdiction data</h3>
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

                <h3>Account management</h3>
                <p>
                    ReportStream will manually manage user accounts for your
                    team. To add or remove team members,{" "}
                    <a href="/support/contact" className="usa-link">
                        contact us
                    </a>
                    .
                </p>

                <h3>Password reset</h3>
                <ul>
                    <li>
                        If you forgot your password, follow the instructions
                        under "Need help signing in?" on the login page at{" "}
                        <NavLink to="/login" key="login" className="usa-link">
                            reportstream.cdc.gov/login
                        </NavLink>
                        .
                    </li>
                    <li>
                        If you want to update your password, log out of the
                        application and use the password reset process outlined
                        above.
                    </li>
                </ul>

                <h4>Support</h4>
                <p>
                    Do you have questions, problems, or bugs to report? Contact
                    us for help.
                </p>
                <a
                    href="/support/contact"
                    className="usa-button usa-button--outline"
                >
                    Contact us
                </a>

                <p className="margin-top-8">
                    <a href="#anchor-top" className="usa-link">
                        Back to top
                    </a>
                </p>
            </section>
        </>
    );
};
