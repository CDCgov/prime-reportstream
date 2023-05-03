import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import React from "react";

import { FeatureName } from "../../AppRouter";

export function Dashboard() {
    return (
        <GridContainer containerSize="widescreen">
            <Helmet>
                <title>{FeatureName.DASHBOARD}</title>
            </Helmet>
            <div>Dashboard!</div>
        </GridContainer>
    );
}
