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
                    ReportStream is a free, open-source data platform that makes it easy for public health data to be transferred from organizations and testing facilities to public health departments.
                </p>
                <h3 className="margin-top-4">Why submit data with ReportStream?</h3>
                <ul>                    
                    <li>Meet your reporting requirements through a single connection. ReportStream is working with <a href="/how-it-works/where-were-live">jurisdictions across the country</a> to route your data where it needs to go.</li>
                    <li>Test results and patient data are securely stored and protected by two-factor authentication, database encryption, and HTTPS.</li> 
                    <li>Created by the CDC and developed for COVID-19 test data, ReportStream is 100% free. </li>
                </ul> 


                <h3>How do I submit data through ReportStream?</h3>
                <p>ReportStream can receive report data as either <a href="https://en.wikipedia.org/wiki/Comma-separated_values" target="_blank" rel="noreferrer noopener">comma-separated values (CSV)</a> or <a href="https://www.hl7.org/" target="_blank" rel="noreferrer noopener">Health Level 7 (HL7)</a> files via a variety of methods.</p>
                <p>Not sure which method is right for you? Contact us as <a href={"mailto:" + DOMPurify.sanitize(site.orgs.RS.email) + "?subject=Getting started with ReportStream"} className="usa-link">{DOMPurify.sanitize(site.orgs.RS.email)}</a> to learn more.</p>
                <h4>Electronic Laboratory Reporting (ELR)</h4>
                <p>Depending on the needs of your organization or facility, ReportStream can configure an ELR connection with your existing systems. ReportStream has established connections with large organizations, test manufacturers, and facilities with advanced systems.</p>
                <h4>CSV upload <span className="text-secondary bg-white border-1px border-secondary font-body-3xs padding-x-1 padding-y-05 text-secondary margin-left-2 text-ttbottom">Pilot program </span></h4>
                <p>Use a simple online tool to submit a CSV formatted with a standard schema. Receive real-time validation and feedback on file format and field values before submission. This feature is currently being piloted in select jurisdictions with organinzations or facilities that have existing Electronic Medical Record (EMR) systems.</p>
                <h4>SimpleReport</h4>
                <p>A partner project under PRIME, SimpleReport ...</p>

                <h3 className="margin-top-6 padding-top-6 border-top-1px border-base-lighter">Get started with ReportStream</h3>
                <p className="margin-bottom-4">Ready to get started or just have
                more questions? Email us at <a href={"mailto:" + DOMPurify.sanitize(site.orgs.RS.email) + "?subject=Getting started with ReportStream"} className="usa-link">{DOMPurify.sanitize(site.orgs.RS.email)}</a> and we’ll follow up with next steps.</p>
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
