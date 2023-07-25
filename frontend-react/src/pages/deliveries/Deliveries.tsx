import React from "react";
import { Helmet } from "react-helmet-async";
import { GridContainer } from "@trussworks/react-uswds";

import HipaaNotice from "../../components/HipaaNotice";
import Title from "../../components/Title";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import { FeatureName } from "../../AppRouter";

import DeliveriesTable from "./Table/DeliveriesTable";

function Deliveries() {
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails || {};
    return (
        <GridContainer>
            <Helmet>
                <title>{FeatureName.DAILY_DATA}</title>
            </Helmet>
            <article className="padding-bottom-5 tablet:padding-top-6">
                <Title preTitle={description} title={FeatureName.DAILY_DATA} />
                {withCatchAndSuspense(<DeliveriesTable />)}
                <HipaaNotice />
            </article>
        </GridContainer>
    );
}

export const DeliveriesWithAuth = () => (
    <AuthElement
        element={<Deliveries />}
        requiredUserType={MemberType.RECEIVER}
    />
);
