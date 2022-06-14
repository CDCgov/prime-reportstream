import { Helmet } from "react-helmet";
import DOMPurify from "dompurify";

import site from "../../../content/site.json";

/* eslint-disable jsx-a11y/anchor-has-content */
export const FacilitiesOverview = () => {
    return (
        <>
            <Helmet>
                <title>
                    Organizations and testing facilities | Getting started |{" "}
                    {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>
            <section id="anchor-top">
                <h2 className="margin-top-0">Overview</h2>
                <p className="usa-intro text-base padding-bottom-4 margin-bottom-4 border-bottom-1px border-base-lighter">
                    ReportStream is a free, open-source data platform that makes
                    it easy for public health data to be transferred from
                    organizations and testing facilities to public health
                    departments.
                </p>
                <h3 className="margin-top-4">
                    Why submit data with ReportStream?
                </h3>
                <ul>
                    <li>
                        Meet your reporting requirements through a single
                        connection. ReportStream is working with{" "}
                        <a
                            href="/how-it-works/where-were-live"
                            className="usa-link"
                        >
                            jurisdictions across the country
                        </a>{" "}
                        to route your data where it needs to go.
                    </li>
                    <li>
                        Test results and patient data are securely stored and
                        protected by two-factor authentication, database
                        encryption, and HTTPS.
                    </li>
                    <li>
                        <a href="/how-it-works/about" className="usa-link">
                            Created by the CDC and developed for COVID-19 test
                            data
                        </a>
                        , ReportStream is 100% free.{" "}
                    </li>
                </ul>

                <h3>How do I submit data through ReportStream?</h3>
                <p>
                    ReportStream can receive report data as either{" "}
                    <a
                        href="https://en.wikipedia.org/wiki/Comma-separated_values"
                        target="_blank"
                        rel="noreferrer noopener"
                    >
                        comma-separated values (CSV)
                    </a>{" "}
                    or{" "}
                    <a
                        href="https://www.hl7.org/"
                        target="_blank"
                        rel="noreferrer noopener"
                    >
                        Health Level 7 (HL7)
                    </a>{" "}
                    files via a variety of methods. All submissions are subject
                    to jurisdictional approval.
                </p>
                <p>
                    Not sure which method is right for you? Contact us as{" "}
                    <a
                        href={
                            "mailto:" +
                            DOMPurify.sanitize(site.orgs.RS.email) +
                            "?subject=Getting started with ReportStream"
                        }
                        className="usa-link"
                    >
                        {DOMPurify.sanitize(site.orgs.RS.email)}
                    </a>{" "}
                    to learn more.
                </p>
                <ul>
                    <li>
                        <h4>Electronic Laboratory Reporting (ELR) via API</h4>
                        <p>
                            Depending on the needs of your organization or
                            facility, ReportStream can configure an ELR
                            connection with your existing systems. ReportStream
                            has established connections with large
                            organizations, test manufacturers, and facilities
                            with advanced systems.
                        </p>
                        <p>
                            For more information, read the&nbsp;
                            <a
                                href="/assets/pdf/ReportStream-Programmers-Guide-v2.2.pdf"
                                className="usa-link"
                                rel="noopener noreferrer"
                                target="_blank"
                            >
                                ReportStream programmer's guide for testing
                                facilities and senders (PDF, updated June 2022)
                            </a>
                        </p>
                    </li>
                    <li>
                        <h4>
                            CSV upload{" "}
                            <span className="text-secondary bg-white border-1px border-secondary font-body-3xs padding-x-1 padding-y-05 text-secondary margin-left-2 text-ttbottom">
                                Pilot program{" "}
                            </span>
                        </h4>
                        <p>
                            Use a simple online tool to submit a CSV file
                            formatted with a standard schema. Receive real-time
                            validation and feedback on file format and field
                            values.
                        </p>
                        <p>
                            {" "}
                            Learn how{" "}
                            <a
                                href="/getting-started/testing-facilities/csv-upload-guide"
                                className="usa-link"
                            >
                                CSV upload
                            </a>{" "}
                            works or read the{" "}
                            <a
                                href="/getting-started/testing-facilities/csv-schema"
                                className="usa-link"
                            >
                                full documentation
                            </a>
                            .
                        </p>
                        <p>
                            <em>
                                This feature is currently being piloted in
                                select jurisdictions with organizations or
                                facilities that have existing Electronic Medical
                                Record (EMR) systems. Pilot partners are
                                selected by recommendation from jurisdictions.
                                Find out if your jurisdiction is{" "}
                                <a
                                    href="/how-it-works/where-were-live"
                                    className="usa-link"
                                >
                                    partnered
                                </a>{" "}
                                with ReportStream and{" "}
                                <a
                                    href={
                                        "mailto:" +
                                        DOMPurify.sanitize(site.orgs.RS.email) +
                                        "?subject=Getting started with ReportStream"
                                    }
                                    className="usa-link"
                                >
                                    contact us
                                </a>{" "}
                                to learn more.{" "}
                            </em>
                        </p>
                    </li>
                    <li>
                        <h4>SimpleReport</h4>
                        <p>
                            <a
                                href="https://simplereport.gov"
                                target="_blank"
                                rel="noreferrer noopener"
                                className="usa-link"
                            >
                                SimpleReport
                            </a>{" "}
                            is a fast, free, and easy way for COVID-19 testing
                            facilities to report results to public health
                            departments. Skip re-entering the same data every
                            time you report. Just pull up a name, enter the test
                            result, and submit. SimpleReport automatically
                            converts your data into the format required by your
                            public health department. Real-time reporting
                            submits test results the moment you record them —
                            saving you time.
                        </p>
                    </li>
                </ul>

                <h3 className="margin-top-6 padding-top-6 border-top-1px border-base-lighter">
                    Get started with ReportStream
                </h3>
                <p className="margin-bottom-4">
                    Ready to get started or just have more questions? Email us
                    at{" "}
                    <a
                        href={
                            "mailto:" +
                            DOMPurify.sanitize(site.orgs.RS.email) +
                            "?subject=Getting started with ReportStream"
                        }
                        className="usa-link"
                    >
                        {DOMPurify.sanitize(site.orgs.RS.email)}
                    </a>{" "}
                    and we'll follow up with next steps.
                </p>
                <p>
                    <a
                        href={
                            "mailto:" +
                            DOMPurify.sanitize(site.orgs.RS.email) +
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
