import { NetworkErrorBoundary } from "rest-hooks";
import { Helmet } from "react-helmet";
import { Suspense } from "react";

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
            <section className="grid-container margin-bottom-5">
                <h3 className="margin-bottom-0">
                    <Suspense fallback={<Spinner />} />
                </h3>
            </section>
            <NetworkErrorBoundary
                fallbackComponent={() => <ErrorPage type="message" />}
            >
                <Suspense fallback={<Spinner />}>
                    <section className="grid-container margin-top-0" />
                    <AdminReceiverDashboard />
                </Suspense>
            </NetworkErrorBoundary>
            <HipaaNotice />
        </>
    );
}
