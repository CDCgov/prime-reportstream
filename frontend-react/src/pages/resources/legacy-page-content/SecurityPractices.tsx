import { Link } from "react-router-dom";

import { BasicHelmet } from "../../../components/header/BasicHelmet";

export const SecurityPracticesIa = () => {
    return (
        <>
            <BasicHelmet pageTitle="Security practices | Resources" />
            <h1 id="anchor-top">Security practices</h1>
            <h2 className="usa-intro text-base">
                Answers to common questions about ReportStream security and data
                practices.
            </h2>

            <section>
                <h3>
                    Does ReportStream have security audits and attestations?
                </h3>
                <p>
                    As a project of the U.S. federal government, ReportStream
                    complies with the Federal Information System Modernization
                    Act (FISMA) of 2014.
                </p>
                <p>
                    FISMA sets standards and requires federal agencies to
                    implement information security plans to protect sensitive
                    data. FISMA and the National Institute of Standards and
                    Technology (NIST) set the FISMA compliance guidelines. More
                    specifically, NIST guidelines:
                </p>
                <ul>
                    <li>
                        Set minimum requirements for information security plans
                        and procedures.
                    </li>
                    <li>
                        Recommend security systems, software, etc. that agencies
                        must implement and approve.
                    </li>
                    <li>
                        Standardize the risk assessment process and set varying
                        standards of information security based on agency risk
                        assessments.
                    </li>
                </ul>
                <p>
                    As part of the FISMA process, ReportStream has full
                    authority to operate (ATO). This authority was granted by
                    CDC's Chief Information Officer/Authorizing Official after
                    an extensive review process documenting over 500 security
                    controls, reviews by multiple teams, and penetration testing
                    by third parties.
                </p>
                <p>
                    Learn more about the FISMA process and the NIST guidance:{" "}
                    <a
                        href="https://csrc.nist.gov/projects/risk-management"
                        className="usa-link"
                        target="_blank"
                        rel="noreferrer"
                    >
                        NIST Risk Management Framework (CSRC)
                    </a>
                </p>
            </section>
            <section>
                <h3>Is ReportStream FedRAMP approved?</h3>
                <p>
                    Federal information systems like ReportStream are not in the
                    scope of the Federal Risk and Authorization Management
                    Program (FedRAMP), which is for vendors that sell cloud
                    products or services to the Federal government. However, as
                    mentioned above, ReportStream follows the FISMA law and both
                    FISMA and FedRAMP share the same NIST security guidelines
                    and standards.
                </p>
            </section>
            <section>
                <h3>How is ReportStream hosted?</h3>
                <p>
                    ReportStream is hosted in a secured FedRAMP accredited
                    environment.
                </p>
            </section>
            <section>
                <h3>Is ReportStream data encrypted in transit?</h3>
                <p>
                    All data is encrypted in transit. ReportStream also uses
                    data-in-transit encryption for connections to send test
                    results to states.
                </p>
            </section>
            <section>
                <h3>Is ReportStream data encrypted at rest? </h3>
                <p>
                    All data is encrypted when stored using facilities of the
                    secured FedRAMP-accredited environment.
                </p>
            </section>
            <section>
                <h3>Who has access to ReportStream data?</h3>
                <p>
                    ReportStream filters and routes data to state and county
                    Public Health Agencies through their electronic reporting
                    interfaces. In addition, Public Health Agencies may
                    designate users who can download ReportStream data from a
                    ReportStream website. There are a number of security
                    controls placed on these user accounts including
                    multi-factor authentication and automatic disablement due to
                    in-activity.
                </p>
                <p>
                    Some members of the PRIME ReportStream team have
                    administrative access to ReportStream data for the purposes
                    of reliably operating the ReportStream service. All
                    ReportStream team members who have this administrative
                    access go through Federal government background checks.{" "}
                </p>
            </section>
            <section>
                <h3>Does ReportStream keep an audit trail?</h3>
                <p>
                    For business and security purposes, ReportStream keeps many
                    different logs of activity. In particular, ReportStream
                    keeps a line-level audit trail of all test results that is
                    sent to ReportStream and all the receivers of data from
                    ReportStream. The ReportStream audit trail is kept for
                    multiple years.
                </p>
            </section>
            <section>
                <h3>How long is data stored?</h3>
                <p>
                    ReportStream is a data conduit , not a store of record.
                    However, to operate reliably, it is necessary to temporarily
                    retain data in ReportStream. The current retention period
                    for test results and other personal identifiable information
                    (PII) and protected health information (PHI) is 30 days.
                </p>
            </section>
            <section>
                <h3>Does ReportStream have terms of service?</h3>
                <p>
                    All organizations that send and receive test results through
                    ReportStream to public health agencies are governed under
                    these{" "}
                    <Link to="/terms-of-service" className="usa-link">
                        terms of service
                    </Link>
                    .
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
