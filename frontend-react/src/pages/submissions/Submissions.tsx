import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";

import SubmissionTable from "./SubmissionTable";
import HipaaNotice from "../../components/HipaaNotice";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary/RSErrorBoundary";
import Title from "../../components/Title";
import useOrganizationSettings from "../../hooks/api/organizations/UseOrganizationSettings/UseOrganizationSettings";

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
                    <meta
                        property="og:image"
                        content="/assets/img/opengraph/howwehelpyou-3.png"
                    />
                    <meta
                        property="og:image:alt"
                        content="An abstract illustration of screens and a document."
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
