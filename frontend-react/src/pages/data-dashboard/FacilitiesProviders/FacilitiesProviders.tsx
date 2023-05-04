import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import React from "react";

import { FeatureName } from "../../../AppRouter";

export function FacilitiesProviders() {
    return (
        <GridContainer containerSize="widescreen">
            <Helmet>
                <title>{FeatureName.FACILITIES_PROVIDERS}</title>
            </Helmet>
            <div>Facilities & Providers</div>
        </GridContainer>
    );
}
