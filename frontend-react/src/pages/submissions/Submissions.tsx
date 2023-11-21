import { Helmet } from "react-helmet-async";
import { GridContainer } from "@trussworks/react-uswds";

import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import HipaaNotice from "../../components/HipaaNotice";
import Title from "../../components/Title";
import { FeatureName } from "../../utils/FeatureName";
import { useSessionContext } from "../../contexts/Session";

import SubmissionTable from "./SubmissionTable";

function SubmissionHistoryPage() {
    const { user } = useSessionContext();
    const { data: orgDetails } = useOrganizationSettings(user.organization);
    const { description } = orgDetails ?? {};

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

export default SubmissionHistoryPage;
