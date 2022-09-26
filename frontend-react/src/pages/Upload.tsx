import { Alert } from "@trussworks/react-uswds";
import { NavLink } from "react-router-dom";
import DOMPurify from "dompurify";
import React from "react";

import { MemberType } from "../hooks/UseOktaMemberships";
import { AuthElement } from "../components/AuthElement";

const TransitionBanner = () => {
    return (
        <Alert
            type="warning"
            heading="CSV uploader has moved to SimpleReport"
            headingLevel="h4"
        >
            <ul>
                <li className="margin-bottom-2">
                    Starting <b>July 18, 2022</b>, the CSV uploader feature will
                    no longer be available from the ReportStream web portal.
                    However, you’ll still have access to your{" "}
                    <NavLink to="/submissions">CSV submission history</NavLink>{" "}
                    through your ReportStream user account until further notice.
                </li>
                <li className="margin-bottom-2">
                    Visit the new CSV uploader by{" "}
                    <a
                        href="https://www.simplereport.gov/app/results/upload"
                        className="usa-link"
                    >
                        following this link
                    </a>{" "}
                    (you can use your existing username and password). If you
                    need assistance or have questions, please email the
                    SimpleReport team at{" "}
                    <a
                        href={
                            "mailto:" +
                            DOMPurify.sanitize("support@simplereport.gov")
                        }
                        className="usa-link"
                    >
                        support@simplereport.gov
                    </a>
                    .
                </li>
            </ul>
        </Alert>
    );
};

export const Upload = () => {
    return (
        <div className="grid-container usa-section margin-bottom-10">
            <section className="margin-bottom-4">
                <TransitionBanner />
            </section>
        </div>
    );
};

export const UploadWithAuth = () => (
    <AuthElement element={<Upload />} requiredUserType={MemberType.SENDER} />
);
