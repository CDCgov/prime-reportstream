import { Suspense } from "react";
import { NetworkErrorBoundary } from "rest-hooks";
import HipaaNotice from "../../components/HipaaNotice";
import TableReports from "./Table/TableReports";
import Spinner from "../../components/Spinner";
import { useOrgName } from "../../utils/OrganizationUtils";
import { Helmet } from "react-helmet";
import { Alert } from "@trussworks/react-uswds";

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
        <NetworkErrorBoundary
            fallbackComponent={() => {
                return <section className="grid-container margin-bottom-5"><Alert type="error">
                    Failed to load data because of network error
                </Alert></section>;
            }}>
            <Helmet>
                <title>Daily data | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <section className="grid-container margin-bottom-5">
                <Suspense fallback={<span className="text-normal text-base">Loading Info...</span>}>
                    <h3 className="margin-bottom-0">
                        <OrgName />
                    </h3>

                </Suspense>
                <h1 className="margin-top-0 margin-bottom-0">COVID-19</h1>
            </section>
            <Suspense fallback={<Spinner />}>
                <section className="grid-container margin-top-0" />
                <TableReports />
            </Suspense>
            <HipaaNotice />
        </NetworkErrorBoundary>
    );
};

export default Daily
