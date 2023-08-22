/* eslint-disable jsx-a11y/anchor-is-valid */
/* eslint-disable jsx-a11y/anchor-has-content */
import DOMPurify from "dompurify";
import { Button, Icon } from "@trussworks/react-uswds";
import { useNavigate } from "react-router-dom";
import { Helmet } from "react-helmet-async";

import site from "../../../content/site.json";
import { ResourcesDirectories } from "../../../content/resources";
import { USExtLink, USLink } from "../../../components/USLink";

export const ELRChecklistIa = () => {
    const navigate = useNavigate();
    return (
        <>
            <Helmet>
                <title>{`${ResourcesDirectories.ELR_CHECKLIST} | Resources`}</title>
            </Helmet>
            <h1 id="anchor-top">{ResourcesDirectories.ELR_CHECKLIST}</h1>
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
            <Button
                type="button"
                onClick={() =>
                    window.open(
                        DOMPurify.sanitize(site.forms.intakeElr.url),
                        "_blank",
                    )
                }
            >
                ELR onboarding form <Icon.Launch />
            </Button>
            <Button
                type="button"
                outline
                onClick={() => navigate("/support/contact")}
            >
                Contact us
            </Button>
            <hr />
            <p className="margin-top-6">
                <strong>On this page:</strong>
            </p>
            <ul>
                <li>
                    <USLink href="#elr-contact-information">
                        ELR contact information
                    </USLink>
                </li>
                <li>
                    <USLink href="#alternate-contact-information">
                        Program or admin staff contact information
                    </USLink>
                </li>
                <li>
                    <USLink href="#data-requirements-and-preferences">
                        Data requirements and preferences
                    </USLink>
                </li>
                <li>
                    <USLink href="#testing-facility-registration">
                        Testing facility registration
                    </USLink>
                </li>
                <li>
                    <USLink href="#hl7-data-fields">
                        HL7 data fields (Not applicable if using an alternate
                        data format)
                    </USLink>
                </li>
                <li>
                    <USLink href="#sftp-details">SFTP details</USLink>
                </li>
                <li>
                    <USLink href="#document-uploads">Document uploads</USLink>
                </li>
            </ul>

            <p className="margin-bottom-4">
                Before gathering information on the checklist or completing the
                ELR onboarding form, we recommend first reviewing information on
                our{" "}
                <USLink href="/getting-started/public-health-departments/overview">
                    Getting started page
                </USLink>{" "}
                and the technical details outlined in{" "}
                <USLink href="/how-it-works/systems-and-settings">
                    Systems & settings
                </USLink>{" "}
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
                    <USExtLink href="https://hl7.org/">HL7</USExtLink> file via
                    Secure File Transfer Protocol (SFTP). We capture Health and
                    Human Services (HHS) required fields, including “Ask on
                    Order Entry” questions.{" "}
                </p>

                <p>
                    Please note: ReportStream follows industry standards for{" "}
                    <USExtLink href="https://github.com/CDCgov/prime-data-hub/blob/production/prime-router/docs/schema_documentation/primedatainput-pdi-covid-19.md">
                        data formatting
                    </USExtLink>{" "}
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
                        <USExtLink href="https://www.lyniate.com/knowledge-hub/hl7-batch-file-protocol/">
                            using FHS and BHS segments
                        </USExtLink>
                        ?{" "}
                    </li>
                    <li>
                        <span className="text-bold">Timing:</span> Do you want
                        real time or daily reports, and at what time of day?{" "}
                    </li>
                    <li>
                        <span className="text-bold">“Ask on Order Entry”:</span>{" "}
                        Do you want “Ask on Order Entry” questions as{" "}
                        <USExtLink href="https://hl7-definition.caristix.com/v2/HL7v2.5/Segments/OBX">
                            OBX fields
                        </USExtLink>
                        ?{" "}
                    </li>
                    <li>
                        <span className="text-">Test data:</span> Do you require
                        synthetic and/or real patient data before going live?{" "}
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
                    <USLink href="https://www.cdc.gov/clia/about.html">
                        CLIA
                    </USLink>{" "}
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
                    <li>Receiving application name</li>
                    <li>
                        Receiving application{" "}
                        <USExtLink href="https://www.hl7.org/oid/">
                            OID
                        </USExtLink>
                    </li>
                    <li>Receiving facility name</li>
                    <li>
                        Receiving facility{" "}
                        <USExtLink href="https://www.hl7.org/oid/">
                            OID
                        </USExtLink>
                    </li>
                </ul>
            </section>
            <section>
                <h3 id="sftp-details">SFTP details </h3>

                <ul>
                    <li>Staging host name (DNS name)</li>
                    <li>Staging folder (folder name)</li>
                    <li>Prod host name (DNS name)</li>
                    <li>Prod host folder (folder name)</li>
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
                    <USExtLink href="https://prime.powerappsportals.us/siw/">
                        ReportStream ELR onboarding form
                    </USExtLink>
                    . We'll get back to you within a week.
                </p>

                <p>
                    Have questions or aren't quite ready for our ELR connection?
                    Send us an email and we'll help you figure out next steps.
                </p>

                <p>
                    <Button
                        type="button"
                        outline
                        onClick={() => navigate("/support/contact")}
                    >
                        Get in touch
                    </Button>
                </p>
            </section>
        </>
    );
};
