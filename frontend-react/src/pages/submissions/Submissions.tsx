import React from "react";

import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import HipaaNotice from "../../components/HipaaNotice";
import Title from "../../components/Title";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { BasicHelmet } from "../../components/header/BasicHelmet";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";

import SubmissionTable from "./SubmissionTable";

function SubmissionHistoryContent() {
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails || {};

    return (
        <>
            <BasicHelmet pageTitle="Submissions" />
            <section className="grid-container margin-top-5">
                <Title title="Submission History" preTitle={description} />
            </section>
            <SubmissionTable />
            <HipaaNotice />
        </>
    );
}

const SubmissionHistory = () =>
    withCatchAndSuspense(<SubmissionHistoryContent />);
export const SubmissionsWithAuth = () => (
    <AuthElement
        element={<SubmissionHistory />}
        requiredUserType={MemberType.SENDER}
    />
);
