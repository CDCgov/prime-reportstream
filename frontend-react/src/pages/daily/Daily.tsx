import { Suspense } from "react";
import { Helmet } from "react-helmet";

import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";
import { useOrgName } from "../../utils/OrganizationUtils";
import ErrorBoundary from "../../components/ErrorBoundary";
import { ErrorPage } from "../error/ErrorPage";

import TableReports from "./Table/TableReports";

const OrgName = () => {
    const orgName: string = useOrgName();
    return (
        <span id="orgName" className="text-normal text-base">
            {orgName}
        </span>
    );
};

function Daily() {
    return (
        <ErrorBoundary fallback={<ErrorPage type="page" />}>
            <Helmet>
                <title>Daily data | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <section className="grid-container margin-bottom-5">
                <h3 className="margin-bottom-0">
                    <Suspense
                        fallback={
                            <span className="text-normal text-base">
                                Loading Info...
                            </span>
                        }
                    >
                        <ErrorBoundary
                            fallback={
                                <span className="text-normal text-base">
                                    Error: not found
                                </span>
                            }
                        >
                            <OrgName />
                        </ErrorBoundary>
                    </Suspense>
                </h3>
                <h1 className="margin-top-0 margin-bottom-0">COVID-19</h1>
            </section>
            <Suspense fallback={<Spinner />}>
                <ErrorBoundary fallback={<ErrorPage type="message" />}>
                    <section className="grid-container margin-top-0" />
                    <TableReports />
                </ErrorBoundary>
            </Suspense>
            <HipaaNotice />
        </ErrorBoundary>
    );
}

export default Daily;
