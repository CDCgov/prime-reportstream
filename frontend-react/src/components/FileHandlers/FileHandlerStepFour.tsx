import React from "react";
import { USExtLink } from "../USLink";
import site from "../../content/site.json";

import { FileSuccessDisplay } from "./FileHandlerMessaging";

export interface FileHandlerStepFourProps {
    destinations?: string;
    successTimestamp?: string;
    reportId?: string;
}

export const FileHandlerStepFour = ({
    destinations,
    successTimestamp,
    reportId,
}: FileHandlerStepFourProps) => {
    return (
        <div className="grid-col flex-1 display-flex flex-column">
            {/* {isFileSuccess && warnings.length === 0 && (
                
            )} */}
            <FileSuccessDisplay
                extendedMetadata={{
                    destinations,
                    timestamp: successTimestamp,
                    reportId,
                }}
                heading="File validated"
                message="Your file is correctly formatted for ReportStream."
                showExtendedMetadata={false}
            />
            <p className="margin-top-4">
                To continue your onboarding, email{" "}
                <USExtLink href={`mailto: ${site.orgs.RS.email}`}>
                    reportstream@cdc.gov
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
};
