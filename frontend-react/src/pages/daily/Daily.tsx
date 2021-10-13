import { Suspense } from "react";
import { NetworkErrorBoundary } from "rest-hooks";
import { Helmet } from "react-helmet";
import { Alert } from "@trussworks/react-uswds";

import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";
import { useOrgName } from "../../utils/OrganizationUtils";
import Title from "../../components/Title";

import TableReports from "./Table/TableReports";

function Daily() {
    const orgName: string = useOrgName();
    return (
        <NetworkErrorBoundary
            fallbackComponent={() => {
                return (
                    <section className="grid-container margin-bottom-5">
                        <Alert type="error">
                            Failed to load data because of network error
                        </Alert>
                    </section>
                );
            }}
        >
            <Helmet>
                <title>Daily data | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <div className="grid-container">
                <Suspense
                    fallback={
                        <span className="text-normal text-base">
                            Loading Info...
                        </span>
                    }
                >
                    <Title title="Covid-19" preTitle={orgName} />
                </Suspense>
                <Suspense fallback={<Spinner />}>
                    <section className="margin-top-0" />
                    <TableReports />
                </Suspense>
                <HipaaNotice />
            </div>
        </NetworkErrorBoundary>
    );
}

export default Daily;
