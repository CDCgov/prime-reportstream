import React, { useMemo } from "react";

import HipaaNotice from "../../components/HipaaNotice";
import { useOrgName } from "../../hooks/UseOrgName";
import Title from "../../components/Title";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { BasicHelmet } from "../../components/header/BasicHelmet";
import { useSessionContext } from "../../contexts/SessionContext";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";
import { NoServicesBanner } from "../../components/alerts/NoServicesAlert";

import DeliveriesTable from "./Table/DeliveriesTable";

function Deliveries() {
    const orgName: string = useOrgName();
    const { activeMembership, initialized } = useSessionContext();
    const notReadyToLoad = useMemo(
        () => activeMembership?.allServices === undefined && initialized,
        [activeMembership?.allServices, initialized]
    );
    return (
        <>
            <BasicHelmet pageTitle="Daily Data" />
            <section className="grid-container margin-bottom-5 tablet:margin-top-6">
                <Title preTitle={orgName} title="Daily Data" />
            </section>
            {notReadyToLoad ? (
                <div className={"grid-container"}>
                    <NoServicesBanner
                        featureName={"deliveries"}
                        organization={orgName}
                        serviceType={"receiver"}
                    />
                </div>
            ) : (
                withCatchAndSuspense(<DeliveriesTable />)
            )}
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
