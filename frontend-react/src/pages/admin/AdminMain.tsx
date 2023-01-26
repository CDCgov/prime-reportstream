import { Suspense } from "react";

import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";
import { OrgsTable } from "../../components/Admin/OrgsTable";
import { AuthElement } from "../../components/AuthElement";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { BasicHelmet } from "../../components/header/BasicHelmet";
import { FeatureName } from "../../AppRouter";
import RSErrorBoundary from "../../components/RSErrorBoundary";

export function AdminMain() {
    return (
        <>
            <BasicHelmet pageTitle={FeatureName.ADMIN} />
            <RSErrorBoundary>
                <Suspense fallback={<Spinner />}>
                    <section className="grid-container margin-top-0" />
                    <OrgsTable />
                </Suspense>
            </RSErrorBoundary>
            <HipaaNotice />
        </>
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
