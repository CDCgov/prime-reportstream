import { Suspense } from "react";
import { Helmet } from "react-helmet";
import { NetworkErrorBoundary } from "rest-hooks";

import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";
import { ErrorPage } from "../error/ErrorPage";
import { OrgsTable } from "../../components/Admin/OrgsTable";
import { AuthElement } from "../../components/AuthElement";
import { MemberType } from "../../hooks/UseOktaMemberships";

export function AdminMain() {
    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <Helmet>
                <title>Admin | {process.env.REACT_APP_TITLE}</title>
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
                    <OrgsTable />
                </Suspense>
            </NetworkErrorBoundary>
            <HipaaNotice />
        </NetworkErrorBoundary>
    );
}

export function AdminMainWithAuth() {
    return (
        <AuthElement
            element={<AdminMain />}
            requiredUserType={MemberType.PRIME_ADMIN}
        />
    );
}
