import { Helmet } from "react-helmet";
import { NetworkErrorBoundary } from "rest-hooks";

import { useOrgName } from "../../hooks/UseOrgName";
import { ErrorPage } from "../error/ErrorPage";
import HipaaNotice from "../../components/HipaaNotice";
import Title from "../../components/Title";

import SubmissionTable from "./SubmissionTable";

function Submissions() {
    const orgName: string = useOrgName();

    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <Helmet>
                <title>Submissions | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <section className="grid-container margin-top-5">
                <Title title="COVID-19" preTitle={orgName} />
            </section>
            <SubmissionTable />
            <HipaaNotice />
        </NetworkErrorBoundary>
    );
}

export default Submissions;
