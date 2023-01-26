import React from "react";

import { StaticAlert, StaticAlertType } from "../StaticAlert";

export default function AdminFetchAlert() {
    return (
        <StaticAlert
            type={StaticAlertType.Error}
            heading="Cannot fetch Organization data as admin"
            message="Please try again as an Organization"
        />
    );
}
