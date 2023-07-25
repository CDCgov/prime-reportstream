import { Helmet } from "react-helmet-async";

import { ResourcesDirectories } from "../../../content/resources";
import { USExtLink, USLink } from "../../../components/USLink";

export const SystemAndSettingsIa = () => {
    return (
        <>
            <Helmet>
                <title>{`${ResourcesDirectories.SYSTEM} | Resources`}</title>
            </Helmet>
            <h1 id="anchor-top">{ResourcesDirectories.SYSTEM}</h1>

            <h2>
                Information about the ReportStream platform, including data
                configuration, formats, and transport
            </h2>

            <p className="text-bold">On this page:</p>
            <ul>
                <li>
                    <USLink href="#data-security-and-storage">
                        Data security and storage
                    </USLink>
                </li>
                <li>
                    <USLink href="#data-configuration-and-formatting">
                        Data configuration and formatting
                    </USLink>
                </li>
                <li>
                    <USLink href="#data-transport-and-sending">
                        Data transport and sending
                    </USLink>
                </li>
                <li>
                    <USLink href="#file-testing-and-launch">
                        File testing and launch
                    </USLink>
                </li>
                <li>
                    <USLink href="#data-access">Data access</USLink>
                </li>
            </ul>

            <section>
                <h3 id="data-security-and-storage">
                    Data security and storage
                </h3>

                <p>
                    All ReportStream data is encrypted in transit and rest using
                    a TLS 1.2+ Azure standard. It is routed through a CDC cloud
                    environment, where it is deleted after 30 days. Additional
                    encryption can be provided if required by public health
                    departments.
                </p>

                <p>
                    As a project of the U.S. federal government, ReportStream
                    also complies with the{" "}
                    <USExtLink href="https://csrc.nist.gov/Projects/risk-management/fisma-background">
                        Federal Information System Management Act
                    </USExtLink>{" "}
                    (FISMA). FISMA sets standards and requires federal agencies
                    to implement information security plans to protect sensitive
                    data.
                </p>

                <p>
                    Get more information on{" "}
                    <USLink href="/resources/security-practices">
                        ReportStream security practices
                    </USLink>
                    .
                </p>
            </section>
            <section>
                <h3 id="data-configuration-and-formatting">
                    Data configuration and formatting
                </h3>

                <p>ReportStream submits data via HL7, version 2.5.1.</p>

                <p>
                    ReportStream data follows{" "}
                    <USExtLink href="https://www.hhs.gov/answers/is-additional-information-including-technical-specifications-available-to-support-laboratories-with-implementation/index.html">
                        HHS guidance for COVID-19 Lab Data Reporting
                    </USExtLink>
                    , including the recommended Health and Human Services
                    COVID-19 “Ask on Order Entry” questions. Sending Ask on
                    Order Entry questions (via{" "}
                    <USExtLink href="https://hl7-definition.caristix.com/v2/HL7v2.5/Segments/OBX">
                        OBX fields
                    </USExtLink>
                    ) is optional.
                </p>

                <p>
                    Public health departments can request data be sent via other
                    file formats such as CSV. Setting up alternative formats is
                    possible, but requires more time throughout the data
                    integration process.
                </p>
            </section>
            <section>
                <h3 id="data-transport-and-sending">
                    Data transport and sending
                </h3>

                <p>
                    ReportStream uses a Secure File Transfer Protocol (SFTP)
                    standard Electronic Laboratory Reporting (ELR) connection.
                    Public health departments using the ReportStream ELR can
                    request data be sent in real time, or batched according to
                    your needs.
                </p>

                <p>
                    Public health departments can request alternative transport
                    platforms such as VPN, PHINMS, SOAP, etc. Setting up
                    alternative formats is possible, but requires more time
                    throughout the data integration process.
                </p>
            </section>
            <section>
                <h3 id="file-testing-and-launch">File testing and launch</h3>

                <p>
                    ReportStream can send a variety of data types to a public
                    health department test environment to make sure everything
                    is working correctly before launch.
                </p>

                <ul>
                    <li>
                        <span className="text-bold">
                            Synthetic data (standard):
                        </span>{" "}
                        ReportStream always sends synthetic data in our{" "}
                        <USExtLink href="https://github.com/CDCgov/prime-reportstream/blob/production/prime-router/docs/schema_documentation/primedatainput-pdi-covid-19.md">
                            standard data schema
                        </USExtLink>{" "}
                        for your review.
                    </li>
                    <li>
                        <span className="text-bold">
                            Patient data (as needed):
                        </span>{" "}
                        If required, ReportStream can also send patient data. In
                        some cases, there may not be a testing facility that is
                        registered with ReportStream in your jurisdiction. In
                        this case, we'll finish all of the possible steps on
                        your side. Once a testing facility registers we'll
                        notify you, then move to approval and launch.
                    </li>
                </ul>
            </section>
            <section>
                <h3>Approval and launch</h3>

                <p>
                    For the first testing facility, we'll send patient data to
                    your test environment for approval before going live with
                    launch. After that, you'll get data automatically for any
                    new or existing testing facilities in your jurisdiction.
                </p>
            </section>
            <section>
                <h3 id="data-access">Data access</h3>

                <p>
                    Once state public health departments join ReportStream
                    you'll get data about all patients who are residents of that
                    state. You'll also get data from testing facilities
                    connected to ReportStream that are located in that state.
                </p>

                <p>
                    ReportStream can also send data to county, city, local,
                    territorial, and tribal public health departments, based on
                    state data access rules and regulations.
                </p>
            </section>
        </>
    );
};
