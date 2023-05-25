import React from "react";
import { Helmet } from "react-helmet-async";
import { GridContainer } from "@trussworks/react-uswds";

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
        <GridContainer>
            <article className="padding-top-5">
                <Helmet>
                    <title>{FeatureName.SUBMISSIONS}</title>
                </Helmet>
                <Title title="Submission History" preTitle={description} />
                <SubmissionTable />
                <HipaaNotice />
            </article>
        </GridContainer>
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
