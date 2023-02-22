import React from "react";
import { Helmet } from "react-helmet-async";

import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import HipaaNotice from "../../components/HipaaNotice";
import Title from "../../components/Title";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";
import { FeatureName } from "../../AppRouter";

import SubmissionTable from "./SubmissionTable";

function SubmissionHistoryContent() {
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails || {};

    return (
        <>
            <Helmet>
                <title>{FeatureName.SUBMISSIONS}</title>
            </Helmet>
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
