import { Helmet } from "react-helmet";
import DOMPurify from "dompurify";

import site from "../../content/site.json";

export const About = () => {
    return (
        <>
            <Helmet>
                <title>About | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <section
                id="anchor-top"
                className="usa-section margin-y-0 padding-top-0 tablet:padding-bottom-2 measure-5"
            >
                <h1 className="margin-top-0 font-body-xl">About</h1>

                <p className="usa-intro text-base">
                    ReportStream is an open source, cloud based platform that
                    aggregates and delivers COVID-19 test results to health
                    departments. We send data directly from testing facilities,
                    labs, and more through a single connection.
                </p>

                <a
                    href={
                        DOMPurify.sanitize(site.pdfPath) + "PRIME-1-pager.pdf"
                    }
                    className="usa-button usa-button--outline margin-bottom-2 tablet:margin-bottom-0"
                >
                    Download PRIME 1-pager
                </a>
                <a
                    href={"mailto:" + DOMPurify.sanitize(site.orgs.RS.email)}
                    className="usa-button usa-button--outline"
                >
                    Contact us
                </a>

                <div className="margin-top-6 padding-top-4 border-top border-base-lighter">
                    <p>
                        ReportStream (also known as PRIME ReportStream) was
                        created for the public good by the{" "}
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
                        , ReportStream helps to streamline and improve public
                        health reporting during the COVID-19 pandemic, and
                        beyond.
                    </p>

                    <p>
                        The U.S. Digital Service is a group of technologists
                        from diverse backgrounds working across the federal
                        government to transform critical services for the
                        people. Specialists join for tours of civic service to
                        create a steady influx of fresh perspectives. USDS
                        continues its non-partisan mission by bringing better
                        government services to all Americans through design and
                        technology.
                    </p>
                </div>
            </section>
        </>
    );
};
