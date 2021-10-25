// @ts-nocheck // TODO: fix types in this file
import { useResource } from "rest-hooks";
import { Suspense } from "react";

import ReportResource from "../../resources/ReportResource";
import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";

import Summary from "./Summary";
import ReportDetails from "./ReportDetails";
import FacilitiesTable from "./FacilitiesTable";

function useQuery() {
    let query = window.location.search.slice(1);
    const queryMap = {};
    Object.assign(
        queryMap,
        ...query
            .split(",")
            .map((s) => s.split("="))
            .map(([k, v]) => ({ [k]: v }))
    );
    return queryMap;
}

const DetailsContent = () => {
    let queryMap = useQuery();
    let reportId = queryMap["reportId"];
    let report = useResource(ReportResource.list(), { sortBy: undefined }).find(
        (r) => r.reportId === reportId
    );

    return (
        <>
            <Summary report={report} />
            <ReportDetails report={report} />
            <FacilitiesTable reportId={report?.reportId || ""} />
            <HipaaNotice />
        </>
    );
};

/* INFO
   This has to exist because the Suspense catch was messing with our ability to offer
   the undefined route option in React Router. The Suspense must be one level above the
   component loading data (i.e. DetailsContent), but could not exist in App because of
   the bug it caused with providing the empty Route to redirect to the 404 page.
   
   >>> Kevin Haube, Sept 30, 2021
*/
export const Details = () => {
    return (
        <Suspense fallback={<Spinner fullPage />}>
            <DetailsContent />
        </Suspense>
    );
};
