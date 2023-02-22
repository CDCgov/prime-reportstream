import { Helmet } from "react-helmet-async";

import site from "../../../content/site.json";
import { ResourcesDirectories } from "../../../content/resources";
import { USExtLink, USLink } from "../../../components/USLink";

/* eslint-disable jsx-a11y/anchor-has-content */
export const GettingStartedPhd = () => {
    return (
        <>
            <Helmet>
                <title>{`${ResourcesDirectories.GETTING_STARTED_PHD} | Resources`}</title>
            </Helmet>
            <h1 id="anchor-top">{ResourcesDirectories.GETTING_STARTED_PHD}</h1>
            <h2>
                A step-by-step process for connecting your jurisdiction to
                ReportStream
            </h2>
            <hr />
            <h3>Overview</h3>
            <p>
                An Electronic Lab Reporting (ELR) connection allows public
                health authorities to automatically receive digital lab reports
                from ReportStream via a secure server.
            </p>
            <p>
                Connecting to ReportStream is similar to setting up an ELR feed
                with a lab or hospital. There are just three steps:
            </p>
            <ol className="usa-process-list">
                <li className="usa-process-list__item">
                    <h3 className="usa-process-list__heading">
                        Schedule kick-off:
                    </h3>
                    <p className="margin-top-05">
                        Let us know you're interested by filling out and
                        submitting{" "}
                        <USExtLink href={site.forms.intakeElr.url}>
                            this form
                        </USExtLink>{" "}
                        . We'll be in touch within a week to schedule a kick-off
                        meeting and start the integration process.
                    </p>
                </li>
                <li className="usa-process-list__item">
                    <h3 className="usa-process-list__heading">
                        Connect and test:
                    </h3>
                    <p>
                        As part of kick-off, you'll share your SFTP server
                        credentials. We'll then send test files and ensure the
                        test data arrives correctly in your database.
                    </p>
                </li>
                <li className="usa-process-list__item">
                    <h3 className="usa-process-list__heading">
                        Launch and roll out:
                    </h3>
                    <p>
                        We'll start sending you real data from testing
                        facilities. As new data is sent in to ReportStream,
                        we'll continually batch and send you only the data
                        relevant to your jurisdiction.
                    </p>
                </li>
            </ol>
            <hr />
            <h3 id="connecting-to-testing-facilities">
                Connecting to testing facilities
            </h3>
            <p>
                New testing facilities join ReportStream on a regular basis
                through our partner SimpleReport or through other public health
                data aggregators and apps. After they join, their data will be
                sent automatically to the correct public health department(s) in
                their jurisdiction who are part of ReportStream. Data is
                accessible via the ReportStream website or the ELR connection.
            </p>
            <p>
                Interested in downloading data directly from the ReportStream
                website? Visit the ReportStream Data{" "}
                <USLink href="/resources/data-download-guide">
                    Manual Download Guide
                </USLink>{" "}
                for more info.
            </p>
            <section>
                <h3>Get started with ReportStream</h3>
                <p>
                    Ready to bring ReportStream to your jurisdiction, or just
                    have more questions? Fill out{" "}
                    <USExtLink href={site.forms.intakeElr.url}>
                        the form
                    </USExtLink>{" "}
                    and we'll be in touch!
                </p>
            </section>
        </>
    );
};
