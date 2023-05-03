import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import React from "react";

import { FeatureName } from "../../../AppRouter";

export function ReportDetails() {
    return (
        <GridContainer containerSize="widescreen">
            <Helmet>
                <title>{FeatureName.REPORT_DETAILS}</title>
            </Helmet>
            <div>Report Details</div>
        </GridContainer>
    );
}
