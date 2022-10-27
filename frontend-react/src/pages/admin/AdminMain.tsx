import { Suspense } from "react";
import { NetworkErrorBoundary } from "rest-hooks";

import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";
import { ErrorPage } from "../error/ErrorPage";
import { OrgsTable } from "../../components/Admin/OrgsTable";
import { AuthElement } from "../../components/AuthElement";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { BasicHelmet } from "../../components/header/BasicHelmet";

export function AdminMain() {
    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <BasicHelmet pageTitle="Admin" />
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
