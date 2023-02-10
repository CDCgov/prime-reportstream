import { Alert } from "@trussworks/react-uswds";
import React from "react";

import { MemberType } from "../hooks/UseOktaMemberships";
import { AuthElement } from "../components/AuthElement";
import { USExtLink, USLink } from "../components/USLink";

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
                    <USLink href="/submissions">CSV submission history</USLink>{" "}
                    through your ReportStream user account until further notice.
                </li>
                <li className="margin-bottom-2">
                    Visit the new CSV uploader by{" "}
                    <USLink href="https://www.simplereport.gov/app/results/upload">
                        following this link
                    </USLink>{" "}
                    (you can use your existing username and password). If you
                    need assistance or have questions, please email the
                    SimpleReport team at{" "}
                    <USExtLink href={`mailto: support@simplereport.gov`}>
                        support@simplereport.gov
                    </USExtLink>
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
