import React from "react";

import { StaticAlert, StaticAlertType } from "../StaticAlert";
import { Link } from "../../shared/Link/Link";

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
                Read more about{" "}
                <Link href="/resources/api/getting-started#set-up-authentication">
                    your next steps for setting up authentication
                </Link>
                .
            </p>
        </div>
    );
}
