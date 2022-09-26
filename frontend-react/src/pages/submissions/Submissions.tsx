import { NetworkErrorBoundary } from "rest-hooks";
import React from "react";

import { useOrgName } from "../../hooks/UseOrgName";
import { ErrorPage } from "../error/ErrorPage";
import HipaaNotice from "../../components/HipaaNotice";
import Title from "../../components/Title";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { BasicHelmet } from "../../components/header/BasicHelmet";

import SubmissionTable from "./SubmissionTable";

function Submissions() {
    const orgName: string = useOrgName();

    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <BasicHelmet pageTitle="Submissions" />
            <section className="grid-container margin-top-5">
                <Title title="COVID-19" preTitle={orgName} />
            </section>
            <SubmissionTable />
            <HipaaNotice />
        </NetworkErrorBoundary>
    );
}

export default Submissions;

export const SubmissionsWithAuth = () => (
    <AuthElement
        element={<Submissions />}
        requiredUserType={MemberType.SENDER}
    />
);
