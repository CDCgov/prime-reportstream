import { Suspense } from "react";
import { Helmet } from "react-helmet";
import { NetworkErrorBoundary } from "rest-hooks";

import { useOrgName } from "../../hooks/UseOrgName";
import Spinner from "../../components/Spinner";
import { ErrorPage } from "../error/ErrorPage";
import HipaaNotice from "../../components/HipaaNotice";
import Title from "../../components/Title";
import FilterContext from "../../contexts/FilterContext";

import SubmissionTable from "./SubmissionTable";
import SubmissionFilters from "./SubmissionFilters";

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
                <SubmissionFilters />
                <NetworkErrorBoundary
                    fallbackComponent={() => <ErrorPage type="message" />}
                >
                    <Suspense fallback={<Spinner />}>
                        <SubmissionTable />
                    </Suspense>
                </NetworkErrorBoundary>
            </FilterContext>
            <HipaaNotice />
        </NetworkErrorBoundary>
    );
}

export default Submissions;
