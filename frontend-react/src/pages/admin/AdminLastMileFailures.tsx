import { NetworkErrorBoundary } from "rest-hooks";
import { Helmet } from "react-helmet";
import { Suspense } from "react";

import { ErrorPage } from "../error/ErrorPage";
import Spinner from "../../components/Spinner";
import HipaaNotice from "../../components/HipaaNotice";
import { AdminLastMileFailuresTable } from "../../components/Admin/AdminLastMileFailuresTable";

export function AdminLastMileFailures() {
    return (
        <>
            <Helmet>
                <title>Admin | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <Suspense fallback={<Spinner />}>
                <section className="grid-container margin-top-0" />
                <NetworkErrorBoundary
                    fallbackComponent={(props) => (
                        <ErrorPage type="message" error={props.error} />
                    )}
                >
                    <AdminLastMileFailuresTable />
                </NetworkErrorBoundary>
            </Suspense>

            <HipaaNotice />
        </>
    );
}
