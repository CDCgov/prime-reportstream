import { BasicHelmet } from "../../../components/header/BasicHelmet";

export const SystemAndSettingsIa = () => {
    return (
        <>
            <BasicHelmet pageTitle="System and Settings | Resources" />
            <h1 id="anchor-top">System and Settings</h1>

            <h2>
                Information about the ReportStream platform, including data
                configuration, formats, and transport
            </h2>

            <p className="text-bold">On this page:</p>
            <ul>
                <li>
                    <a href="#data-security-and-storage" className="usa-link">
                        Data security and storage
                    </a>
                </li>
                <li>
                    <a
                        href="#data-configuration-and-formatting"
                        className="usa-link"
                    >
                        Data configuration and formatting
                    </a>
                </li>
                <li>
                    <a href="#data-transport-and-sending" className="usa-link">
                        Data transport and sending
                    </a>
                </li>
                <li>
                    <a href="#file-testing-and-launch" className="usa-link">
                        File testing and launch
                    </a>
                </li>
                <li>
                    <a href="#data-access" className="usa-link">
                        Data access
                    </a>
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
                    <a
                        href="https://csrc.nist.gov/Projects/risk-management/fisma-background"
                        className="usa-link"
                    >
                        Federal Information System Management Act
                    </a>{" "}
                    (FISMA). FISMA sets standards and requires federal agencies
                    to implement information security plans to protect sensitive
                    data.
                </p>

                <p>
                    Get more information on{" "}
                    <a
                        href="/resources/security-practices"
                        className="usa-link"
                    >
                        ReportStream security practices
                    </a>
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
                    <a
                        href="https://www.hhs.gov/answers/is-additional-information-including-technical-specifications-available-to-support-laboratories-with-implementation/index.html"
                        className="usa-link"
                    >
                        HHS guidance for COVID-19 Lab Data Reporting
                    </a>
                    , including the recommended Health and Human Services
                    COVID-19 “Ask on Order Entry” questions. Sending Ask on
                    Order Entry questions (via{" "}
                    <a
                        href="https://hl7-definition.caristix.com/v2/HL7v2.5/Segments/OBX"
                        className="usa-link"
                    >
                        OBX fields
                    </a>
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
                        <a
                            href="https://github.com/CDCgov/prime-reportstream/blob/production/prime-router/docs/schema_documentation/primedatainput-pdi-covid-19.md"
                            target="_blank"
                            rel="noreferrer"
                            className="usa-link"
                        >
                            standard data schema
                        </a>{" "}
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
