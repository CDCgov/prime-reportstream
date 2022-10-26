import React from "react";

import HipaaNotice from "../../components/HipaaNotice";
import { useOrgName } from "../../hooks/UseOrgName";
import Title from "../../components/Title";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { BasicHelmet } from "../../components/header/BasicHelmet";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";

import DeliveriesTable from "./Table/DeliveriesTable";

function Deliveries() {
    const orgName: string = useOrgName();
    return (
        <>
            <BasicHelmet pageTitle="Daily Data" />
            <section className="grid-container margin-bottom-5 tablet:margin-top-6">
                <Title preTitle={orgName} title="Daily Data" />
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
