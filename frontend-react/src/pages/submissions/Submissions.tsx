import { Suspense } from "react";
import { Helmet } from "react-helmet";
import { NetworkErrorBoundary } from "rest-hooks";

import { useOrgName } from "../../utils/OrganizationUtils";
import Spinner from "../../components/Spinner";
import { ErrorPage } from "../error/ErrorPage";
import HipaaNotice from "../../components/HipaaNotice";
import Title from "../../components/Title";

import SubmissionTable from "./SubmissionTable";
import SubmissionFilters from "./SubmissionFilters";
import FilterContext from "./FilterContext";

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
            <FilterContext>
                <NetworkErrorBoundary
                    fallbackComponent={() => <ErrorPage type="message" />}
                >
                    <Suspense fallback={<Spinner />}>
                        <SubmissionFilters />
                        <SubmissionTable />
                    </Suspense>
                </NetworkErrorBoundary>
            </FilterContext>
            <HipaaNotice />
        </NetworkErrorBoundary>
    );
}

export default Submissions;
