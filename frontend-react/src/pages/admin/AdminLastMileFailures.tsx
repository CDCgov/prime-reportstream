import React, { Suspense } from "react";
import { Helmet } from "react-helmet-async";

import Spinner from "../../components/Spinner";
import HipaaNotice from "../../components/HipaaNotice";
import { AdminLastMileFailuresTable } from "../../components/Admin/AdminLastMileFailuresTable";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { FeatureName } from "../../AppRouter";

export function AdminLastMileFailures() {
    return (
        <>
            <Helmet>
                <title>{FeatureName.ADMIN}</title>
            </Helmet>
            <section className="grid-container margin-bottom-5">
                <h3 className="margin-bottom-0">
                    <Suspense fallback={<Spinner />} />
                </h3>
            </section>
            <section className="grid-container margin-top-0" />
            <AdminLastMileFailuresTable />
            <HipaaNotice />
        </>
    );
}

export function AdminLMFWithAuth() {
    return (
        <AuthElement
            element={<AdminLastMileFailures />}
            requiredUserType={MemberType.PRIME_ADMIN}
        />
    );
}
