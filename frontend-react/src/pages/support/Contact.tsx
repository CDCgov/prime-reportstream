import { Helmet } from "react-helmet";
import DOMPurify from "dompurify";

import site from "../../content/site.json";

export const Contact = () => {
    return (
        <>
            <Helmet>
                <title>Contact | Support | {process.env.REACT_APP_TITLE}</title>
            </Helmet>

            <h1>Contact us</h1>
            <h2>
                Want to get in touch with ReportStream? Email us at{" "}
                <a
                    href={"mailto:" + DOMPurify.sanitize(site.orgs.RS.email)}
                    className="usa-link"
                >
                    {site.orgs.RS.email}
                </a>
                .
            </h2>
            <section>
                <h3>Interested in partnering with ReportStream?</h3>
                <p>
                    Read our getting started guides for{" "}
                    <a
                        className="usa-link"
                        href="/getting-started/public-health-departments/overview"
                    >
                        public health departments
                    </a>{" "}
                    and{" "}
                    <a
                        className="usa-link"
                        href="/getting-started/testing-facilities/overview"
                    >
                        organizations and testing facilities
                    </a>
                    .
                </p>
                <p>
                    To learn more, or to begin the account registration process,
                    email us at{" "}
                    <a
                        href={
                            "mailto:" + DOMPurify.sanitize(site.orgs.RS.email)
                        }
                        className="usa-link"
                    >
                        {site.orgs.RS.email}
                    </a>
                    .
                </p>
            </section>
            <section>
                {" "}
                <h3>
                    Have a problem with a data you're trying to send or receive?
                </h3>
                <p>
                    Read our guides for sending data{" "}
                    <a
                        className="usa-link"
                        href="/getting-started/public-health-departments/elr-checklist"
                    >
                        via API (ELR)
                    </a>{" "}
                    or{" "}
                    <a
                        className="usa-link"
                        href="/getting-started/testing-facilities/csv-upload-guide"
                    >
                        via CSV upload
                    </a>
                    . For all other issues, contact us at{" "}
                    <a
                        href={
                            "mailto:" + DOMPurify.sanitize(site.orgs.RS.email)
                        }
                        className="usa-link"
                    >
                        {site.orgs.RS.email}
                    </a>
                    .
                </p>
            </section>
            <section>
                <h3 className="margin-top-4">Need something else?</h3>
                <p>
                    For quick answers to common questions, read our{" "}
                    <a className="usa-link" href="/support/faq">
                        frequently asked questions
                    </a>
                    .
                </p>
                <p>
                    If you have another question, issue, or bug to report, we're
                    happy to help. Email us at{" "}
                    <a
                        href={
                            "mailto:" + DOMPurify.sanitize(site.orgs.RS.email)
                        }
                        className="usa-link"
                    >
                        {site.orgs.RS.email}
                    </a>
                    .
                </p>
            </section>
        </>
    );
};
