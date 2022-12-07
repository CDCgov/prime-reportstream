import { Link } from "react-router-dom";
import DOMPurify from "dompurify";

import site from "../../../content/site.json";
import { BasicHelmet } from "../../../components/header/BasicHelmet";

export const Contact = () => {
    return (
        <>
            <BasicHelmet pageTitle="Contact | Support" />
            <h1>Contact us</h1>
            <h2>
                For general inquiries, questions, or issues. Reach out, we're
                happy to help!
            </h2>
            <section>
                <h3>Interested in partnering with ReportStream? </h3>
                <p>
                    If you want to learn more about ReportStream or how to get
                    started, email us at{" "}
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
                <p>
                    To register for a free user account, follow the steps
                    outlined in our{" "}
                    <Link
                        to="/resources/account-registration-guide"
                        key="account registration guide"
                        className="usa-link"
                    >
                        account registration guide
                    </Link>
                    .
                </p>
            </section>
            <section>
                {" "}
                <h3>
                    Have a problem with a data you're trying to send or receive?
                </h3>
                <p>
                    Read our guides for sending data via{" "}
                    <a className="usa-link" href="/resources/elr-checklist">
                        ELR
                    </a>{" "}
                    or{" "}
                    <a className="usa-link" href="/resources/programmers-guide">
                        API
                    </a>
                    .
                </p>
                <p>
                    If you are having issues with an existing connection, use
                    our{" "}
                    <a className="usa-link" href="/support/service-request">
                        service request form
                    </a>{" "}
                    to open a ticket with our support team.
                </p>
                <p>
                    For all other issues, contact us at{" "}
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
                    Email us at{" "}
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
