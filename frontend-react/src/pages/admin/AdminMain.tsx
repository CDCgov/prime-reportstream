import { Suspense } from "react";
import { Helmet } from "react-helmet";
import { NetworkErrorBoundary } from "rest-hooks";

import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";
import { ErrorPage } from "../error/ErrorPage";
import { OrgsTable } from "../../components/Admin/OrgsTable";

export function AdminMain() {
    return (
        <>
            <Helmet>
                <title>Admin | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <Suspense fallback={<Spinner />}>
                <NetworkErrorBoundary
                    fallbackComponent={(props) => (
                        <ErrorPage type="message" error={props.error} />
                    )}
                >
                    <section className="grid-container margin-top-0" />
                    <OrgsTable />
                </NetworkErrorBoundary>
            </Suspense>
            <HipaaNotice />
        </>
    );
}
