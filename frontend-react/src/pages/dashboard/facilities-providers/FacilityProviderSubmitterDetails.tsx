import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import React from "react";

export function FacilityProviderSubmitterDetails() {
    return (
        // TODO: title will be dynamic based on prop
        <GridContainer containerSize="widescreen">
            <Helmet>
                <title>Facility Details</title>
            </Helmet>
            <div>Facility, provider & submitter details</div>
        </GridContainer>
    );
}
