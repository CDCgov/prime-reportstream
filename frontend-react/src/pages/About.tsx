import { Button } from "@trussworks/react-uswds";
import { useNavigate } from "react-router-dom";
import { Helmet } from "react-helmet-async";

import { USExtLink, USLink } from "../components/USLink";

import site from "./../content/site.json";

export const About = () => {
    const navigate = useNavigate();
    return (
        <>
            <div className="grid-container rs-documentation usa-prose desktop:margin-top-6">
                <Helmet>
                    <title>About</title>
                </Helmet>
                <h1 id="anchor-top">About</h1>
                <h2>
                    ReportStream is an open source, cloud based platform that
                    aggregates and delivers COVID-19 test results to health
                    departments. We send data directly from testing facilities,
                    labs, and more through a single connection.
                </h2>
                <p>
                    <Button
                        type="button"
                        outline
                        onClick={() => navigate("/product")}
                    >
                        Learn more about ReportStream
                    </Button>
                    <Button
                        type="button"
                        outline
                        onClick={() => navigate("/support/contact")}
                    >
                        Contact us
                    </Button>
                </p>
                <hr />
                <p>
                    ReportStream (also known as PRIME ReportStream) was created
                    for the public good by the{" "}
                    <USLink href={site.orgs.CDC.url}>
                        Centers for Disease Control and Prevention (CDC)
                    </USLink>
                    , and the{" "}
                    <USExtLink href={site.orgs.USDS.url}>
                        U.S. Digital Service (USDS)
                    </USExtLink>
                    .
                </p>
                <p>
                    Part of the{" "}
                    <USLink href={site.orgs.PRIME.url}>
                        Pandemic-Ready Interoperability Modernization Effort
                        (PRIME)
                    </USLink>
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
