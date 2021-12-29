import { Suspense } from "react";
import { Helmet } from "react-helmet";
import { NetworkErrorBoundary } from "rest-hooks";

import { useOrgName } from "../../utils/OrganizationUtils";
import Spinner from "../../components/Spinner";

import { ErrorPage } from "../error/ErrorPage";
import SubmissionTable from "./SubmissionsTable";

const OrgName = () => {
    const orgName: string = useOrgName();
    return (
        <span id="orgName" className="text-normal text-base">
            {orgName}
        </span>
    );
};

function Submissions() {
    return (
        <NetworkErrorBoundary
            fallbackComponent={() => <ErrorPage type="page" />}
        >
            <Helmet>
                <title>Submissions | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <section className="grid-container margin-top-5">
                <h3 className="margin-bottom-0">
                    <Suspense
                        fallback={
                            <span className="text-normal text-base">
                                Loading Info...
                            </span>
                        }
                    >
                        <OrgName />
                    </Suspense>
                </h3>
                <h1 className="margin-top-0 margin-bottom-0">COVID-19</h1>
            </section>
            <NetworkErrorBoundary
                        fallbackComponent={() => <ErrorPage type="message" />}
                    >
                <Suspense fallback={<Spinner />}>
                    <SubmissionTable></SubmissionTable>
                </Suspense>
            </NetworkErrorBoundary>
        </NetworkErrorBoundary>
    );
}

export default Submissions;
