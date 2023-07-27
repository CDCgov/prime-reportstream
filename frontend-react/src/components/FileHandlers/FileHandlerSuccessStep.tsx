import React from "react";

import { USExtLink, USLink } from "../USLink";
import site from "../../content/site.json";
import { StaticAlert, StaticAlertType } from "../StaticAlert";

export default function FileHandlerSuccessStep() {
    return (
        <div>
            <div className="margin-bottom-4">
                <StaticAlert
                    type={[StaticAlertType.Success]}
                    heading="File validated"
                    message="Your file is correctly formatted for ReportStream."
                />
            </div>
            <p className="font-size-18 margin-bottom-4">
                To continue your onboarding, email{" "}
                <USExtLink href={`mailto: ${site.orgs.RS.email}`}>
                    {site.orgs.RS.email}
                </USExtLink>{" "}
                and let us know you have validated your file. Our team will
                respond soon to help you get set up in staging.
            </p>
            <p className="font-size-18 margin-0">
                Learn more about your next steps in{" "}
                <USLink href="/resources/api/getting-started#set-up-authentication">
                    the onboarding process
                </USLink>
                .
            </p>
        </div>
    );
}
