import { Helmet } from "react-helmet";
import React from "react";

import HipaaNotice from "../../components/HipaaNotice";
import { AdminReceiverDashboard } from "../../components/Admin/AdminReceiverDashboard";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";

export function AdminReceiverDashPage() {
    return (
        <>
            <Helmet>
                <title>Admin Destination Dashboard</title>
            </Helmet>
            <section className="grid-container margin-top-0" />
            <AdminReceiverDashboard />
            <HipaaNotice />
        </>
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
