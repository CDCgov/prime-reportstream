import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import React from "react";

import { FeatureName } from "../../../AppRouter";

export function FacilityProviderSubmitterDetails() {
    // TODO: title will be dynamic based on prop
    return (
        <GridContainer containerSize="widescreen">
            <Helmet>
                <title>{FeatureName.FACILITY_PROVIDER_SUBMITTER_DETAILS}</title>
            </Helmet>
            <div>Facility, provider & submitter details</div>
        </GridContainer>
    );
}
