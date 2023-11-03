import { Helmet } from "react-helmet-async";
import React from "react";
import { GridContainer } from "@trussworks/react-uswds";

import HipaaNotice from "../../components/HipaaNotice";
import { AdminReceiverDashboard } from "../../components/Admin/AdminReceiverDashboard";

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

export default AdminReceiverDashPage;
