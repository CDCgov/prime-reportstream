import { NetworkErrorBoundary } from "rest-hooks";
import { Helmet } from "react-helmet";
import React, { Suspense } from "react";

import { ErrorPage } from "../error/ErrorPage";
import Spinner from "../../components/Spinner";
import HipaaNotice from "../../components/HipaaNotice";
import { AdminReceiverDashboard } from "../../components/Admin/AdminReceiverDashboard";

export function AdminReceiverDashPage() {
    return (
        <>
            <Helmet>
                <title>Admin Destination Dashboard</title>
            </Helmet>
            <Suspense fallback={<Spinner />}>
                <NetworkErrorBoundary
                    fallbackComponent={(props) => (
                        <ErrorPage type="message" error={props.error} />
                    )}
                >
                    <section className="grid-container margin-top-0" />
                    <AdminReceiverDashboard />
                </NetworkErrorBoundary>
            </Suspense>
            <HipaaNotice />
        </>
    );
}
