import React from "react";
import { Helmet } from "react-helmet-async";
import { GridContainer } from "@trussworks/react-uswds";

import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import HipaaNotice from "../../components/HipaaNotice";
import Title from "../../components/Title";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";

import SubmissionTable from "./SubmissionTable";

function SubmissionHistoryContent() {
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails ?? {};

    return (
        <GridContainer>
            <article className="padding-top-5">
                <Helmet>
                    <title>Submission history</title>
                    <meta
                        name="description"
                        content="The Submission History dashboard provides the status of data you sent through ReportStream."
                    />
                </Helmet>
                <Title title="Submission History" preTitle={description} />
                <SubmissionTable />
                <HipaaNotice />
            </article>
        </GridContainer>
    );
}

const SubmissionHistoryPage = () =>
    withCatchAndSuspense(<SubmissionHistoryContent />);

export default SubmissionHistoryPage;
