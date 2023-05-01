import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import React from "react";

export function FacilityProviderSubmitterDetails() {
    return (
        <GridContainer containerSize="widescreen">
            <Helmet>
                // TODO: will be dynamic based on prop
                <title>Facility Details</title>
            </Helmet>
            <div>Facility, provider & submitter details</div>
        </GridContainer>
    );
}
