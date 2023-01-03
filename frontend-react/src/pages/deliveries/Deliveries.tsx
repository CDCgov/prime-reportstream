import React from "react";

import HipaaNotice from "../../components/HipaaNotice";
import Title from "../../components/Title";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { BasicHelmet } from "../../components/header/BasicHelmet";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import { FeatureName } from "../../AppRouter";

import DeliveriesTable from "./Table/DeliveriesTable";

function Deliveries() {
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails || {};
    return (
        <>
            <BasicHelmet pageTitle={FeatureName.DAILY_DATA} />
            <section className="grid-container margin-bottom-5 tablet:margin-top-6">
                <Title preTitle={description} title={FeatureName.DAILY_DATA} />
            </section>
            {withCatchAndSuspense(<DeliveriesTable />)}
            <HipaaNotice />
        </>
    );
}

export const DeliveriesWithAuth = () => (
    <AuthElement
        element={<Deliveries />}
        requiredUserType={MemberType.RECEIVER}
    />
);
