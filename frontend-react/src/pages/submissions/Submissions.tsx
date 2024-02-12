import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";

import SubmissionTable from "./SubmissionTable";
import HipaaNotice from "../../components/HipaaNotice";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";
import Title from "../../components/Title";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";

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
