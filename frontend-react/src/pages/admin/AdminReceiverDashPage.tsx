import { Helmet } from "react-helmet-async";
import React from "react";
import { GridContainer } from "@trussworks/react-uswds";

import HipaaNotice from "../../components/HipaaNotice";
import { AdminReceiverDashboard } from "../../components/Admin/AdminReceiverDashboard";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";

export function AdminReceiverDashPage() {
    return (
        <GridContainer>
            <Helmet>
                <title>Admin Destination Dashboard</title>
            </Helmet>
            <article>
                <AdminReceiverDashboard />
                <HipaaNotice />
            </article>
        </GridContainer>
    );
}

export function AdminReceiverDashWithAuth() {
    return (
        <AuthElement
            element={<AdminReceiverDashPage />}
            requiredUserType={MemberType.PRIME_ADMIN}
        />
    );
}
