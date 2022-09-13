import { Helmet } from "react-helmet";
import DOMPurify from "dompurify";
import { Alert } from "@trussworks/react-uswds";

import site from "../../../content/site.json";

export const ServiceRequest = () => {
    return (
        <>
            <Helmet>
                <title>
                    Service request | Support | {process.env.REACT_APP_TITLE}
                </title>
            </Helmet>

            <h1>Service request</h1>
            <h2>
                Have an issue with an existing connection? Use this form to open
                a ticket with our support team.
            </h2>

            <Alert type="info" slim>
                Unable to view or submit this form? Have a general question
                about ReportStream? You can also contact us by email at{" "}
                <a
                    href={"mailto:" + DOMPurify.sanitize(site.orgs.RS.email)}
                    className="usa-link"
                >
                    {site.orgs.RS.email}
                </a>
                .
            </Alert>

            <iframe
                title="ReportStream service request form"
                className="form-smartsheet__support"
                src="https://app.smartsheetgov.com/b/form/ff33efa457be461c9893301d4c0ec12d"
            ></iframe>
        </>
    );
};
