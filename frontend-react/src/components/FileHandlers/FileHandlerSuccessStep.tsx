import React from "react";

import { USExtLink } from "../USLink";
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
            <p>
                To continue your onboarding, email{" "}
                <USExtLink href={`mailto: ${site.orgs.RS.email}`}>
                    {site.orgs.RS.email}
                </USExtLink>{" "}
                to let us know you have validated your file. Our team will be in
                touch soon to help you get set up in staging.
            </p>
            <p>
                Learn more about the onboarding process in the API Programmer's
                Guide.
            </p>
        </div>
    );
}
