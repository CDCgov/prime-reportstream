import React, { Suspense } from "react";

import Spinner from "../../components/Spinner";
import HipaaNotice from "../../components/HipaaNotice";
import { AdminLastMileFailuresTable } from "../../components/Admin/AdminLastMileFailuresTable";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { BasicHelmet } from "../../components/header/BasicHelmet";

export function AdminLastMileFailures() {
    return (
        <>
            <BasicHelmet pageTitle="Admin" />
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
