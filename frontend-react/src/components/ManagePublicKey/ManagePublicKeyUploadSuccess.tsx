import React from "react";

import { StaticAlert, StaticAlertType } from "../StaticAlert";
import { USLink } from "../USLink";

export default function ManagePublicKeyUploadSuccess() {
    const heading = "New public key received by ReportStream";

    return (
        <div>
            <div className="margin-bottom-4">
                <StaticAlert
                    type={[StaticAlertType.Success]}
                    heading={heading}
                />
            </div>
            <div className="margin-bottom-4">
                You can now submit data to ReportStream.
            </div>
            <p>
                If you need more information on your next steps, refer to page
                11 in the{" "}
                <USLink href="/resources/programmers-guide">
                    API Programmer’s Guide.
                </USLink>
            </p>
        </div>
    );
}
