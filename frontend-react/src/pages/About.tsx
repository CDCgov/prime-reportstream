import DOMPurify from "dompurify";

import { BasicHelmet } from "../components/header/BasicHelmet";
import { USExtLink, USLink } from "../components/USLink";

import site from "./../content/site.json";

export const About = () => {
    return (
        <>
            <div className="grid-container rs-documentation usa-prose desktop:margin-top-6">
                <BasicHelmet pageTitle="About" />
                <h1 id="anchor-top">About</h1>
                <h2>
                    ReportStream is an open source, cloud based platform that
                    aggregates and delivers COVID-19 test results to health
                    departments. We send data directly from testing facilities,
                    labs, and more through a single connection.
                </h2>
                <p>
                    <button className="usa-button usa-button--outline">
                        <USLink href="/product">
                            Learn more about ReportStream{" "}
                        </USLink>
                    </button>
                    <button className="usa-button usa-button--outline">
                        <USLink href="/support/contact">Contact us</USLink>
                    </button>
                </p>
                <hr />
                <p>
                    ReportStream (also known as PRIME ReportStream) was created
                    for the public good by the{" "}
                    <USExtLink href={DOMPurify.sanitize(site.orgs.CDC.url)}>
                        Centers for Disease Control and Prevention (CDC)
                    </USExtLink>
                    , and the{" "}
                    <USExtLink href={DOMPurify.sanitize(site.orgs.USDS.url)}>
                        U.S. Digital Service (USDS)
                    </USExtLink>
                    .
                </p>
                <p>
                    Part of the{" "}
                    <USExtLink href={DOMPurify.sanitize(site.orgs.PRIME.url)}>
                        Pandemic-Ready Interoperability Modernization Effort
                        (PRIME)
                    </USExtLink>
                    , ReportStream helps to streamline and improve public health
                    reporting during the COVID-19 pandemic, and beyond.
                </p>
                <p>
                    The U.S. Digital Service is a group of technologists from
                    diverse backgrounds working across the federal government to
                    transform critical services for the people. Specialists join
                    for tours of civic service to create a steady influx of
                    fresh perspectives. USDS continues its non-partisan mission
                    by bringing better government services to all Americans
                    through design and technology.
                </p>
            </div>
        </>
    );
};
