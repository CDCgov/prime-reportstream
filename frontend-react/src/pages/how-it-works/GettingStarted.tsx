import { Helmet } from "react-helmet";

import site from "../../content/site.json";

/* eslint-disable jsx-a11y/anchor-has-content */
export const GettingStarted = () => {
    return (
        <>
            <Helmet>
                <title>
                    Getting started | How it works |{" "}
                    {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <section id="anchor-top">
                <h1 className="margin-top-0">Getting started</h1>
                <p className="usa-intro">
                    First, we’ll get you connected to our web application, where
                    you can immediately start downloading reporting data that is
                    relevant to your jurisdiction.
                </p>
                <p className="usa-intro">
                    Depending on your needs, we can also create a customized
                    Electronic Lab Reporting (ELR) connection — an online setup
                    used to send digital lab reports to public health
                    departments via a secure server. Either way, reach out to us
                    and we’ll get back to you in about a week.
                </p>
                <a
                    href={
                        "mailto:" +
                        site.orgs.RS.email +
                        "?subject=Getting started with ReportStream"
                    }
                    className="usa-button usa-button--outline"
                >
                    Contact us
                </a>
                <hr className="margin-y-6" />
                On this page:
                <ul>
                    <li>
                        <a href="#data-download" className="usa-link">
                            Data download
                        </a>
                    </li>
                    <li>
                        <a href="#elr-connection" className="usa-link">
                            ELR connection
                        </a>
                    </li>
                    <li>
                        <a
                            href="#connecting-to-testing-facilities"
                            className="usa-link"
                        >
                            Connecting to testing facilities
                        </a>
                    </li>
                </ul>
            </section>
            <hr className="margin-y-6" />

            <section>
                <h2 id="data-download">Data download</h2>
                Our fastest option to get up and running, our web application
                gives you access to reporting results via manual download from
                our online portal, in just two steps.
                <ol className="usa-process-list">
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Kickoff and onboarding
                        </h4>
                        <p className="margin-top-05">
                            <a
                                href={
                                    "mailto:" +
                                    site.orgs.RS.email +
                                    "?subject=Getting started with ReportStream"
                                }
                                className="margin-right-1 usa-link"
                            >
                                Send us an email
                            </a>
                            with the information below. In most cases we’ll
                            review and approve your account in about a week.
                        </p>
                        <ul>
                            <li>Name of public health department</li>
                            <li>
                                Jurisdiction type (state, county, city, etc.)
                            </li>
                            <li>
                                Name of jurisdiction (For example: “Harris
                                County” or “Washington State”)
                            </li>
                            <li>
                                Name(s) and email(s) of people who need database
                                access
                            </li>
                        </ul>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">Log in</h4>
                        <p>
                            We’ll send you your account information, then simply
                            log in to download your reporting data.
                        </p>
                    </li>
                </ol>
                <h2 id="elr-connection">ELR connection</h2>
                If you’ve decided you’ll need an ELR connection for your public
                health data reporting, we’ll get some information from you on
                your needs and requirements. Connecting with ReportStream is
                similar to setting up an ELR feed with a lab or hospital, and
                takes just a few steps.
                <ol className="usa-process-list">
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Kickoff and onboarding
                        </h4>
                        <p className="margin-top-05">
                            Tell us your data configuration requirements and
                            preferences by submitting our{" "}
                            <a
                                href={site.forms.intakeElr.url}
                                className="usa-link"
                            >
                                ReportStream ELR onboarding form
                            </a>
                            . We’ll review your requirements and reach out to
                            start the data integration process in about a week.
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Connect and test
                        </h4>
                        <p>
                            After sharing credentials for your SFTP server,
                            we’ll work with you to ensure testing facility data
                            arrives correctly in your database. First we’ll send
                            over some test files and after the files are
                            accepted by both ReportStream and the public health
                            department, we’re ready for production, launch, and
                            roll out.
                        </p>
                    </li>
                    <li className="usa-process-list__item">
                        <h4 className="usa-process-list__heading">
                            Launch and roll out
                        </h4>
                        <p>
                            Together we’ll decide on the first testing facility
                            to begin sending you data. Going forward, it’s easy
                            to register new testing facilities as they join
                            ReportStream.
                        </p>
                    </li>
                </ol>
                <h2 id="connecting-to-testing-facilities">
                    Connecting to testing facilities
                </h2>
                <p>
                    New testing facilities join ReportStream on a regular basis,
                    through our partner SimpleReport, or through other public
                    health data aggregators and apps. After they join, their
                    data will be sent automatically to the correct public health
                    department(s) in their jurisdiction who are part of
                    ReportStream. Data is accessible via our web application or
                    the ELR connection.
                </p>
                <p>
                    You can request a weekly email update from ReportStream to
                    notify you when new facilities join in your jurisdiction,
                    and let us know if you have any special requirements for
                    them to complete before they join.
                </p>
                <h3>ELR onboarding checklist</h3>
                <p>
                    Check out our{" "}
                    <a
                        href="/how-it-works/elr-checklist"
                        className="usa-link margin-right-1 usa-link"
                    >
                        ELR onboarding checklist
                    </a>
                    to preview the information you’ll need to complete step one:
                    the{" "}
                    <a href={site.forms.intakeElr.url} className="usa-link">
                        ReportStream ELR onboarding form
                    </a>
                    .
                </p>
                <hr className="margin-y-6" />
                <h2>Get started with ReportStream</h2>
                Ready to bring ReportStream to your jurisdiction or just have
                more questions? Email us and we’ll follow up with next steps.
                <p>
                    <a
                        href={
                            "mailto:" +
                            site.orgs.RS.email +
                            "?subject=Getting started with ReportStream"
                        }
                        className="usa-button usa-button--outline"
                    >
                        Get in touch
                    </a>
                </p>
            </section>
        </>
    );
};
