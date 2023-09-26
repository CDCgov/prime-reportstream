import { Helmet } from "react-helmet-async";

import site from "../../../content/site.json";
import { Link } from "../../../shared/Link/Link";

export const Contact = () => {
    return (
        <>
            <Helmet>
                <title>Contact | Support</title>
            </Helmet>
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
                    <Link href={`mailto: ${site.orgs.RS.email}`}>
                        {site.orgs.RS.email}
                    </Link>
                    .
                </p>
                <p>
                    To register for a free user account, follow the steps
                    outlined in our{" "}
                    <Link
                        href="/resources/account-registration-guide"
                        key="account registration guide"
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
                    <Link href="/resources/elr-checklist">ELR</Link> or{" "}
                    <Link href="/resources/api">API</Link>.
                </p>
                <p>
                    If you are having issues with an existing connection, use
                    our{" "}
                    <Link href="/support/service-request">
                        service request form
                    </Link>{" "}
                    to open a ticket with our support team.
                </p>
                <p>
                    For all other issues, contact us at{" "}
                    <Link href={`mailto: ${site.orgs.RS.email}`}>
                        {site.orgs.RS.email}
                    </Link>
                    .
                </p>
            </section>
            <section>
                <h3 className="margin-top-4">Need something else?</h3>
                <p>
                    For quick answers to common questions, read our{" "}
                    <Link href="/support/faq">frequently asked questions</Link>.
                </p>
                <p>
                    Email us at{" "}
                    <Link href={`mailto: ${site.orgs.RS.email}`}>
                        {site.orgs.RS.email}
                    </Link>
                    .
                </p>
            </section>
        </>
    );
};
