import DOMPurify from "dompurify";

import { BasicHelmet } from "../components/header/BasicHelmet";

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
                    <a
                        href="/product"
                        className="usa-button usa-button--outline"
                    >
                        Learn more about ReportStream{" "}
                    </a>
                    <a
                        href="/support/contact"
                        className="usa-button usa-button--outline"
                    >
                        Contact us
                    </a>
                </p>
                <hr />
                <p>
                    ReportStream (also known as PRIME ReportStream) was created
                    for the public good by the{" "}
                    <a
                        href={DOMPurify.sanitize(site.orgs.CDC.url)}
                        className="usa-link"
                    >
                        Centers for Disease Control and Prevention (CDC)
                    </a>
                    , and the{" "}
                    <a
                        href={DOMPurify.sanitize(site.orgs.USDS.url)}
                        className="usa-link"
                    >
                        U.S. Digital Service (USDS)
                    </a>
                    .
                </p>
                <p>
                    Part of the{" "}
                    <a
                        href={DOMPurify.sanitize(site.orgs.PRIME.url)}
                        className="usa-link"
                    >
                        Pandemic-Ready Interoperability Modernization Effort
                        (PRIME)
                    </a>
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
