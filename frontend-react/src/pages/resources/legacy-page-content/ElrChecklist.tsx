/* eslint-disable jsx-a11y/anchor-is-valid */
/* eslint-disable jsx-a11y/anchor-has-content */
import DOMPurify from "dompurify";

import site from "../../../content/site.json";
import { BasicHelmet } from "../../../components/header/BasicHelmet";

export const ELRChecklistIa = () => {
    return (
        <>
            <BasicHelmet pageTitle="ELR onboarding checklist | Resources" />
            <h1 id="anchor-top">ELR onboarding checklist</h1>
            <h2>
                If you're a public health department and want to connect
                ReportStream through Electronic Lab Reporting (ELR), you'll need
                to fill out the ReportStream ELR onboarding form.
            </h2>
            <h2 className="margin-bottom-8">
                This checklist provides a preview of what we'll ask, so you can
                gather everything you need to complete the form.
            </h2>
            <hr />
            <a
                href={DOMPurify.sanitize(site.forms.intakeElr.url)}
                target="_blank"
                rel="noreferrer"
                className="usa-button margin-bottom-2 tablet:margin-bottom-0"
            >
                ELR onboarding form
            </a>
            <a
                href="/support/contact"
                className="usa-button usa-button--outline"
            >
                Contact us
            </a>
            <hr />
            <p className="margin-top-6">
                <strong>On this page:</strong>
            </p>
            <ul>
                <li>
                    <a href="#elr-contact-information" className="usa-link">
                        ELR contact information
                    </a>
                </li>
                <li>
                    <a
                        href="#alternate-contact-information"
                        className="usa-link"
                    >
                        Program or admin staff contact information
                    </a>
                </li>
                <li>
                    <a
                        href="#data-requirements-and-preferences"
                        className="usa-link"
                    >
                        Data requirements and preferences
                    </a>
                </li>
                <li>
                    <a
                        href="#testing-facility-registration"
                        className="usa-link"
                    >
                        Testing facility registration
                    </a>
                </li>
                <li>
                    <a href="#hl7-data-fields" className="usa-link">
                        HL7 data fields (Not applicable if using an alternate
                        data format)
                    </a>
                </li>
                <li>
                    <a href="#sftp-details" className="usa-link">
                        SFTP details
                    </a>
                </li>
                <li>
                    <a href="#document-uploads" className="usa-link">
                        Document uploads
                    </a>
                </li>
            </ul>

            <p className="margin-bottom-4">
                Before gathering information on the checklist or completing the
                ELR onboarding form, we recommend first reviewing information on
                our{" "}
                <a
                    href="/getting-started/public-health-departments/overview"
                    className="usa-link"
                >
                    Getting started page
                </a>{" "}
                and the technical details outlined in{" "}
                <a
                    href="/how-it-works/systems-and-settings"
                    className="usa-link"
                >
                    Systems & settings
                </a>{" "}
                with your IT and data specialists.
            </p>
            <section>
                <h3 id="elr-contact-information">ELR contact information</h3>

                <p>
                    This is the person who helps set up the ELR connection. It
                    is often an IT person or a third party agency.
                </p>

                <ul>
                    <li>Name</li>
                    <li>Email address</li>
                    <li>Phone number</li>
                </ul>
            </section>
            <section>
                <h3 id="alternate-contact-information">
                    Program or admin staff contact information
                </h3>

                <ul>
                    <li>Name</li>
                    <li>Email address</li>
                    <li>Phone number</li>
                </ul>
            </section>
            <section>
                <h3 id="data-requirements-and-preferences">
                    Data requirements and preferences{" "}
                </h3>

                <p>
                    ReportStream sends data as an{" "}
                    <a href="https://hl7.org/">HL7</a> file via Secure File
                    Transfer Protocol (SFTP). We capture Health and Human
                    Services (HHS) required fields, including “Ask on Order
                    Entry” questions.{" "}
                </p>

                <p>
                    Please note: ReportStream follows industry standards for{" "}
                    <a href="https://github.com/CDCgov/prime-data-hub/blob/production/prime-router/docs/schema_documentation/primedatainput-pdi-covid-19.md">
                        data formatting
                    </a>{" "}
                    and reporting. While we can support custom format or sending
                    mechanisms, this will increase the time required to build
                    your connection.{" "}
                </p>

                <p>Confirm your preferences and provide details for: </p>

                <ul>
                    <li>
                        <span className="text-bold">File type:</span> (HL7
                        v2.5.1, CSV, or other){" "}
                    </li>
                    <li>
                        <span className="text-bold">Transport method:</span>{" "}
                        (SFTP, VPN, Health Information Exchange, SOAP, PHINMS,
                        or other){" "}
                    </li>
                    <li>
                        <span className="text-bold">Batching:</span> Do you want
                        HL7 message batching{" "}
                        <a href="https://www.lyniate.com/knowledge-hub/hl7-batch-file-protocol/">
                            using FHS and BHS segments
                        </a>
                        ?{" "}
                    </li>
                    <li>
                        <span className="text-bold">Timing:</span> Do you want
                        real time or daily reports, and at what time of day?{" "}
                    </li>
                    <li>
                        <span className="text-bold">“Ask on Order Entry”:</span>{" "}
                        Do you want “Ask on Order Entry” questions as{" "}
                        <a href="https://hl7-definition.caristix.com/v2/HL7v2.5/Segments/OBX">
                            OBX fields
                        </a>
                        ?{" "}
                    </li>
                    <li>
                        <span className="text-bold">Test data:</span> Do you
                        require synthetic and/or real patient data before going
                        live?{" "}
                    </li>
                    <li>
                        <span className="text-bold">
                            Local public health department reporting:
                        </span>{" "}
                        Explain any local public health department reporting
                        requirements that are different from the state.{" "}
                    </li>
                    <li>
                        <span className="text-bold">
                            Third-party information services:
                        </span>{" "}
                        Do you require your data to be routed through a
                        third-party like a Health Information Exchange or a
                        health information integration service?{" "}
                    </li>
                    <li>
                        <span className="text-bold">Additional fields:</span>{" "}
                        Any other data or fields you’d like collected?{" "}
                    </li>
                </ul>
            </section>
            <section>
                <h3 id="testing-facility-registration">
                    Testing facility registration{" "}
                </h3>

                <p>
                    ReportStream collects unique identifiers from testing
                    facilities when they register to send data, including name,{" "}
                    <a href="https://www.cdc.gov/clia/about.html">CLIA</a>{" "}
                    number, and address. This information is included with data
                    sent to public health departments.
                </p>

                <ul>
                    <li>
                        <span className="text-bold">
                            Registration requirements:
                        </span>{" "}
                        Do you require testing facilities to register before
                        data is sent to you?{" "}
                    </li>
                    <li>
                        <span className="text-bold">Custom data fields:</span>{" "}
                        Do you require information in addition to standard
                        fields (name, CLIA number and/or address)?{" "}
                    </li>
                    <li>
                        <span className="text-bold">
                            Custom registration process:
                        </span>{" "}
                        Do you have a custom testing facility registration
                        process?{" "}
                    </li>
                </ul>
            </section>
            <section>
                <h3 id="hl7-data-fields">
                    HL7 data fields (Not applicable if using an alternate data
                    format){" "}
                </h3>

                <ul>
                    <li>Receiving application name </li>
                    <li>
                        Receiving application{" "}
                        <a href="https://www.hl7.org/oid/">OID</a>
                    </li>
                    <li>Receiving facility name </li>
                    <li>
                        Receiving facility{" "}
                        <a href="https://www.hl7.org/oid/">OID</a>
                    </li>
                </ul>
            </section>
            <section>
                <h3 id="sftp-details">SFTP details </h3>

                <ul>
                    <li>Staging host name (DNS name)</li>
                    <li>Staging folder (folder name) </li>
                    <li>Prod host name (DNS name) </li>
                    <li>Prod host folder (folder name) </li>
                </ul>
            </section>
            <section>
                <h3 id="document-uploads">Document uploads </h3>

                <ul>
                    <li>
                        <span className="text-bold">
                            Required documentation:
                        </span>{" "}
                        Necessary registration or data use, or legal agreements,
                        etc. that you need ReportStream to complete
                    </li>
                    <li>
                        <span className="text-bold">Sample files:</span> Sample
                        files of your required data formatting, rules or
                        regulations, etc. that would be helpful to review{" "}
                    </li>
                </ul>
            </section>

            <section>
                <h3>Ready for ReportStream?</h3>

                <p>
                    Once you have all the information you need, submit our{" "}
                    <a href="https://prime.powerappsportals.us/siw/">
                        ReportStream ELR onboarding form
                    </a>
                    . We'll get back to you within a week.
                </p>

                <p>
                    Have questions or aren't quite ready for our ELR connection?
                    Send us an email and we'll help you figure out next steps.
                </p>

                <p>
                    <a
                        href="/support/contact"
                        className="usa-button usa-button--outline"
                    >
                        Get in touch
                    </a>
                </p>
            </section>
        </>
    );
};
