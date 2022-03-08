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
import SubmissionContext from "./SubmissionContext";
import PaginatedTable from "../../components/PaginatedTable";


// TEST CODE WOOHOO!
function addOne(val: number) {
    return val + 1
}

const obj = {
    fieldA: "hello",
    fieldB: 122,
    instructions: new Map<string, Function>([
        ["fieldB", addOne]
    ])
}


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
            <SubmissionContext>
                <NetworkErrorBoundary
                    fallbackComponent={() => <ErrorPage type="message" />}
                >
                    <Suspense fallback={<Spinner />}>
                        <SubmissionFilters />
                        {/*<SubmissionTable />*/}
                        <PaginatedTable objects={[obj]}/>
                    </Suspense>
                </NetworkErrorBoundary>
            </SubmissionContext>
            <HipaaNotice />
        </NetworkErrorBoundary>
    );
}

export default Submissions;
